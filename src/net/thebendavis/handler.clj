;; ## Web Request Handlers
;;
;; This namespace includes the logic of the webapp, including routing
;; and functionality.

(ns net.thebendavis.handler
  (:require [ring.util.response :refer [redirect redirect-after-post response]]

            [compojure.core :refer [GET POST defroutes]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]

            [clojure.string :as str]

            [environ.core :refer [env]]

            [hiccup.util :refer [url]]

            [net.thebendavis.views :refer :all]
            [net.thebendavis.system :as sys])
  (:gen-class :main true))

;; `system` holds the global system state (including contacts database)
(def system nil)

;; ## Common Parameter Handling
;;
;; Utility functions for working with the parameters passed around via
;; requests.

(defn params-remap
  "Take a map and two prefixes, get the values associated with keys starting
  with the two prefixes, then zip them together into a new map.

  This is used for matching the values in the forms we get when the user saves a
  contact. For example, when matching the email addresses with the tags
  associated with each one, the prefixes `:email-address` and `:email-note` will
  map the value of `:email-address-1` with `:email-note-1`.


  `(params-remap {:x-1 :apple, :x-2 :banana, :y-1 :red, :y-2 :yellow} :x :y) -> {:apple :red, :banana :yellow}`"
  [params k-prefix v-prefix]

  (letfn [(filter-param-keys
            [kw-prefix]
            (->> (keys params)
                 (filter #(.startsWith (str %1) (str kw-prefix)) )
                 sort))]

    (assert (= (count (filter-param-keys k-prefix))
               (count (filter-param-keys v-prefix))))

    (into {} (for [[kk vk] (map vector
                                (filter-param-keys k-prefix)
                                (filter-param-keys v-prefix))
                   :when (not-empty (params kk))]
               [(params kk) (params vk)]))))

(defn params-reset
  "Create a set from the values in the map with keys starting with the given
  prefix.

  `(params-reset {:nickname-0 \"Jon\", :nickname-1 \"Jonny\") -> #{\"Jon\", \"Jonny\"}`
  "
  [params k-prefix]
  (->> (params-remap params k-prefix k-prefix) keys (into #{})))

;; ## Contact Storage

(defn save-contact
  "Take the parameters for the contact and save it to the db."
  [{:keys [contact-name original-name] :as params}]

  (let [db @(system :db)
        contact-map (-> (db contact-name)
                        (assoc :email (params-remap params :email-address :email-note))
                        (assoc :phone (params-remap params :phone-number  :phone-note))
                        (assoc :nicknames    (params-reset params :nickname))
                        (assoc :affiliations (params-reset params :affiliation))
                        (assoc :birthday (params :birthday))
                        (assoc :address  (params :address))
                        (assoc :note     (params :note)))]

    (if (and (not= original-name contact-name)
             (contains? db contact-name))

      ;; The user changed the name of the contact, but the new name
      ;; already exists in the db. Don't clobber: append "(extra?)"
      ;; and save as a new contact.
      (assoc-in
       (save-contact (assoc params :contact-name (str contact-name " (extra?)")))
       [:flash] "saved as copy, name was already in use")


      ;; Otherwise: remove original name, add in new name (if present), & write.
      (do
        (swap! (system :db) dissoc original-name)

        (when (not-empty contact-name)
          (swap! (system :db) assoc contact-name contact-map))

        ;; write out to disk
        (sys/db-write! system)

        ;; back to the edit page
        (-> (redirect-after-post (str (url "/edit" {:name contact-name})))
            (assoc-in [:flash] (if (not-empty contact-name)
                                 (str "saved contact: " contact-name)
                                 (str "deleted contact: " original-name))))))))

;; ## Contacts management for Mutt
;;
;; These requests are used to provide the interface for mutt users to
;; add and search contacts.

(defn email-id-pairs
  "Make a seq of name->email address pairs.

  Users with more than one email address will show up multiple times: once for
  each email address."
  [db]
  (apply concat
         (for [[name {:keys [email]}] db]
           (for [[addr _] email]
             [name addr]))))

(defn mutt-query
  "Search contacts' names and email addresses and present in a format for
  display in mutt, as described in the
  [mutt manual](http://www.mutt.org/doc/manual/manual-4.html#query)."
  [db query]

  (response
   (->> (email-id-pairs db)
        (sort-by #(.toLowerCase (first %1)))
        (map (fn [[name addr]] (str addr "\t" name)))
        (filter (partial re-find (re-pattern (str "(?i)" query))))
        (str/join "\n")
        (str "\n"))))

(defn add-email
  "Add an email address to the contacts database via POST to /add-email

  If there is already a contact with the given name and email address, do
  nothing.

  If the email exists in a contact with a different name, add the given name as
  a nickname to the existing contact. (Useful for contacts with clients that use
  \"Smith, John\" for \"John Smith\", etc.)

  Otherwise, just insert the email address to the record for the contact with
  the given name, creating a new contact if necessary."
  [name address]

  (let [db @(system :db)
        [existing-name _] (->> (email-id-pairs db)
                               (filter (fn [[n a]] (= a address)))
                               first)]

    (cond
     ;; there is already a contact with this name and email. do nothing.
     (= name existing-name)
     nil

     ;; already have this email address, but contact has different name. add a new nickname
     (not-empty existing-name)
     (swap! (system :db) update-in [existing-name :nicknames] conj name)

     ;; otherwise, just add the email to the given name (creating a contact if necessary)
     :else
     (swap! (system :db) assoc-in
            [name :email address]
            (get-in db [name :email address])))

    ;; write the updated contacts database to disk
    (sys/db-write! system)

    (-> (redirect-after-post "/"))))


;; ## URL Routing Configuration
;;
;; This maps the URL routes available to the functions that handle
;; them.

(defroutes route-handler
  (GET "/" {flash :flash :as request}
       (home-page flash @(system :db)))

  (GET "/new" {flash :flash}
       (edit-contact flash "unnamed contact" {}))

  (GET "/edit" {flash :flash
                {name :name} :params}
       (let [db @(system :db)]
         (if (empty? name)
           (assoc-in (redirect "/") [:flash] flash)
           (when (contains? db name)
             (edit-contact flash name (assoc (db name) :original-name name))))))

  (POST "/save" {params :params :as request}
        (save-contact params))

  (POST "/mutt-query" [q]
        (when (not-empty q)
          (mutt-query @(system :db) q)))

  (POST "/add-email" [name address]
        (when (not-empty address)
          (add-email name address)))

  (route/resources "/")
  (route/not-found not-found-page))


;; ## Whitelisting clients by IP
;;
;; If the user specifies a set of IP addresses on startup, the webapp
;; will only serve requests from those sources.

(def ip-whitelist (atom nil))

(defn wrap-checkip
  "For each request, if there is an ip-whitelist, only serve the request if the
  IP of the request is in the whitelist."
  [handler]
  (fn [{:keys [remote-addr uri] :as req}]
    (let [whitelist @ip-whitelist]
      (if (and (seq whitelist) (not (contains? whitelist remote-addr)))
        (do ;; a whitelist exists but this IP isn't in it
          (println "Blocked request from:" remote-addr "for" uri "- not in whitelist" @ip-whitelist)
          (println req)
          {:status 403
           :body "<!doctype html><html><body><h1>unauthorized</h1></body></html>"})

        ;; either in the whitelist, or there is no whitelist.
        ;; serve the request normally
        (handler req)))))


;; ## Ring Configuration
;;
;; The initialization and handler setup used by the ring web applications library.

(defn init
  "Ring initialization: set the whitelist (if given) and load the database file.

  If the database file does not exist, it will be created. If no file is named,
  the app will use the file `scrolloflex.edn` in the current directory."
  []

  (when (env :whitelist)
    (reset! ip-whitelist (set (str/split (env :whitelist) #"(,|\s)+")))
    (println "only serving requests from whitelisted IPs:" (str/join ", " @ip-whitelist)))

  (->> (or (env :dbfile) "scrolloflex.edn")
       sys/system
       sys/start
       constantly
       (alter-var-root #'system)))

;; the main handler and entry point for the webapp
(def handler
  (wrap-checkip (site route-handler)))
