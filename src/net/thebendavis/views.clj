;; ## Views
;;
;; These templates are specified using
;; [hiccup](https://github.com/weavejester/hiccup).

(ns net.thebendavis.views
  (:require [clojure.string :as str]
            [hiccup.core :refer [h html]]
            [hiccup.form :refer [form-to hidden-field label text-area submit-button text-field]]
            [hiccup.page :refer [html5]]
            [hiccup.util :refer [url]]
            [net.thebendavis.presentation :refer :all]))

(defn page-template
  "The base template for all pages.

  Pass in optional title and dismissable alert (\"flash\") content."
  [{:keys [title flash]} & body]
  (html {:mode :html}
        "<!doctype html>"
        [:html {:lang "en"}
         [:head
          [:title (or title "contacts")]

          [:meta {:charset "utf-8"}]
          [:meta {:name "viewport"
                  :content "width=device-width, initial-scale=1.0"}]

          ;; bootstrap
          [:link {:href  "/css/bootstrap.min.css"
                  :rel   "stylesheet"
                  :media "screen"}]]

         [:body
          ;; page header
          [:div.container
           [:div.page-header
            [:h1 [:a {:href "/"} "contacts "]]]]

          [:div.container
           ;; optional dismissable alert box for one-time messages
           (when flash
             [:div.alert.alert-info.alert-dismissable
              [:button {:type "button" :class "close"
                        :data-dismiss "alert" :aria-hidden "true"} "&times;"]
              [:p (h (str flash))]])
           ;; page body content
           (vec (cons :div.page (-> body concat vec)))]

          [:div.container
           [:div.row
            [:div.col-sm-12
             [:div#footer
              [:div.panel.panel-default
               [:div.panel-footer
                [:a {:href "https://github.com/thebendavis/scrolloflex"} "scrolloflex"]
                " &#169; 2014 "
                [:a {:href "http://www.thebendavis.net"} "Ben Davis"]]]]]]]

          ;; javascript
          [:script {:src "/js/jquery.min.js"}]
          [:script {:src "/js/bootstrap.min.js"}]
          [:script {:src "/js/jquery.tablesorter.min.js"}]
          [:script {:src "/js/jquery.tablesorter.widgets.min.js"}]
          [:script {:src "/js/custom.js"}]]]))


;; 404 page
(def not-found-page
  (page-template {:title "page not found"}
                 [:h1 "page not found :("]))


(defn home-page
  "homepage: list all contacts, with search box and new contact button."
  [flash db]

  (page-template
   {:title "Contacts Home"
    :flash flash}

   [:div.row
    [:div.col-md-3.col-md-push-9
     [:p.text-right
      [:a {:href "/new"}
       [:button.btn.btn-primary.btn-lg
        [:span.glyphicon.glyphicon-plus {:aria-hidden "true"}] " add contact"]]]]
    [:div.col-md-9.col-md-pull-3
     [:input.form-control.input-lg.search {:data-column "all"
                                           :type        "search"
                                           :placeholder "search"
                                           :title       "search"}]]]

   ;; sortable table of all contacts
   [:div.row
    [:table#allcontacts.table.table-hover
     [:thead [:tr [:th "name"] [:th "affiliations"] [:th "email"] [:th "phone"]]]
     [:tbody
      (for [name (sort-by #(.toLowerCase %1) (keys db))
            :let [dest (url "/edit" {:name name})]]
        [:tr
         [:td [:a {:href dest} [:strong (h name)]]]
         [:td [:a {:href dest} (->> (db name) :affiliations sort (str/join ", ") h)]]
         [:td [:a {:href dest} (->> (db name) :email
                                    sort-pairs first first ;; show the first email, sorted by tag first
                                    abbreviate-email h)]]
         [:td [:a {:href dest} (->> (db name) :phone
                                    sort-pairs first first ;; show the first phone #, sorted by tag first
                                    h)]]])]]]))


(defn edit-contact
  "page for displaying and editing a single contact"
  [flash contact-name contact-map]

  (page-template
   {:title (str "Edit: " (h contact-name))
    :flash flash}

   [:form {:action "/save"
           :method "POST"
           :role "form"}

    (when (contact-map :original-name)
      (hidden-field "original-name" (contact-map :original-name)))


    ;; ---------- name ----------
    [:div.row
     [:div.form-group
      [:div.col-sm-9
       [:label.sr-only {:for "contact-name"} "Contact Name:"]
       [:input.form-control.input-lg {:id    "contact-name"
                                      :name  "contact-name"
                                      :type  "text"
                                      :value contact-name}]]
      [:div.col-sm-3
       [:button.form-control.btn.input-lg.btn-lg.btn-block.btn-danger {:type "submit"}
        [:span.glyphicon.glyphicon-floppy-disk {:aria-hidden "true"}] " save contact"]]]]

    [:br]

    [:div.row
     [:div.col-sm-6

      ;; ---------- email addresses ----------
      [:div.well
       [:h3 "email"]
       (for [[n [email email-note]] (number-coll (sort-pairs (contact-map :email)))]
         [:div.row
          [:div.form-group
           [:div.col-xs-8
            [:input.form-control {:id    (str "email-address-" n)
                                  :name  (str "email-address-" n)
                                  :type  "email"
                                  :value email}]]
           [:div.col-xs-4
            [:input.form-control {:id          (str "email-note-" n)
                                  :name        (str "email-note-" n)
                                  :value       email-note
                                  :placeholder "optional note"}]]]])
       [:div.row
        [:div.form-group
         [:div.col-xs-8
          [:input.form-control {:id   "email-address-new"
                                :name "email-address-new"
                                :type "email"}]]
         [:div.col-xs-4
          [:input.form-control {:id          "email-note-new"
                                :name        "email-note-new"
                                :placeholder "optional note"}]]]]]


      ;; ---------- phone numbers ----------
      [:div.well
       [:h3 "phone"]
       (for [[n [phone phone-note]] (number-coll (sort-pairs (contact-map :phone)))]
         [:div.row
          [:div.form-group
           [:div.col-xs-8
            [:input.form-control {:id    (str "phone-number-" n)
                                  :name  (str "phone-number-" n)
                                  :type  "tel"
                                  :value phone}]]
           [:div.col-xs-4
            [:input.form-control {:id          (str "phone-note-" n)
                                  :name        (str "phone-note-" n)
                                  :placeholder "optional note"
                                  :value       phone-note}]]]])
       [:div.row
        [:div.form-group
         [:div.col-xs-8
          [:input.form-control {:id   "phone-number-new"
                                :name "phone-number-new"
                                :type "tel"}]]
         [:div.col-xs-4
          [:input.form-control {:id          "phone-note-new"
                                :name        "phone-note-new"
                                :placeholder "optional note"}]]]]]

      ;; ---------- address ----------
      [:div.well
       [:h3 "address"]
       [:div.form-group
        [:textarea.form-control {:id   "address"
                                 :name "address"
                                 :rows 3}
         (h (contact-map :address))]]]

      ;; ---------- birthday ----------
      [:div.well
       [:h3 "birthday"]
       [:div.form-group
        [:input.form-control {:id          "birthday"
                              :name        "birthday"
                              :type        "date"
                              :placeholder "YYYY-MM-DD"
                              :value       (contact-map :birthday)}]]]]

     ;; ---------- affiliations ----------
     [:div.col-sm-6
      [:div.well
       [:h3 "affiliations"]
       [:div.form-group
        (for [[n affiliation] (number-coll (sort (contact-map :affiliations)))]
          [:input.form-control {:id    (str "affiliation-" n)
                                :name  (str "affiliation-" n)
                                :value affiliation}])
        [:input.form-control {:id   "affiliation-new"
                              :name "affiliation-new"}]]]

      ;; ---------- note ----------
      [:div.well
       [:h3 "note"]
       [:div.form-group
        [:textarea.form-control {:id   "notes"
                                 :name "note"
                                 :rows 10}
         (h (contact-map :note))]]]

      ;; ---------- nicknames ----------
      [:div.well
       [:h3 "nicknames"]
       [:div.form-group
        (for [[n nickname] (number-coll (sort (contact-map :nicknames)))]
          [:input.form-control {:id    (str "nickname-" n)
                                :name  (str "nickname-" n)
                                :value nickname}])
        [:input.form-control {:id   "nickname-new"
                              :name "nickname-new"}]]]]]]))
