;; ## Managed System State
;;
;; This structure contains the state of the system: specifically the
;; contacts data and the file used to persist this data.
;;
;; Use `system` to create a new system, and `start` and `stop` for set
;; up and tear down, respectively.

(ns net.thebendavis.system
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

;; ## Serialization
;;
;; The contacts database is persisted to a file, serialized using the
;; [edn](https://github.com/edn-format/edn) format.

(defn db-reload!
  "Read the contacts database from the file named in the `system`. This will
  create an empty database file if the file does not exist."
  [system]

  (let [db-file (io/file (system :filename))]
    (when-not (.exists db-file)
      (println "initializing new, empty DB file:" (.getCanonicalPath db-file))
      (spit db-file (pr-str {})))

    (let [contents (edn/read-string (slurp db-file))]
      (println "read" (count contents) "entries from" (.getCanonicalPath db-file))
      (reset! (system :db) contents))))

(defn db-write!
  "Serialize contacts database to file. Warning: overwrites contents of file."
  [system]
  (spit (system :filename) (pr-str @(system :db))))

;; ## System Management

(defn system
  "Returns a new instance of the whole application"
  [fname]

  {:filename fname
   :db (atom {})})


(defn start
  "Initialize the system by loading the database content from disk. Returns the
  updated instance of the system."
  [system]

  (db-reload! system)
  system)


(defn stop
  "Shut down the system by persisting the database content to disk. Returns the
  updated instance of the system."
  [system]

  (db-write! system)
  system)
