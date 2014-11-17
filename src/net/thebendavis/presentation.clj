;; ## Presentation Utility Functions
;;
;; Some utility functions used for formatting data in the views.

(ns net.thebendavis.presentation
  (:require [clojure.string :as str]))

(defn sort-pairs
  "Take a map and return a seq of key-value pairs, sorted by the value first, then by key when values match.

  In contrast to normal sorting, here the empty string is sorted
  last. This function is used to sort things like email addresses
  with optional tags: this way tagged entries also show ahead of the
  untagged entries."
  [m]
  (sort (fn [[l-key l-val] [r-key r-val]]
          (cond
           (= l-val r-val) (compare l-key r-key)

           ;; sort empty strings to the end
           (str/blank? l-val) 1
           (str/blank? r-val) -1

           :else (compare l-val r-val)))
        m))

(defn number-coll
  "Turn a collection into a seq of indexed pairs.

  Example: `[:a :b :c :d] -> '([0 :a] [1 :b] [2 :c] [3 :d])`"
  [coll]

  (map vector (range) coll))

(defn abbreviate-email
  "If the email address is too long, abbreviate it as the form
  \"emai...l@add...com\", otherwise leave it as is."
  [email-address]

  (let [width 20
        crop-str (fn [s] (if (<= (count s) width)
                           s ;; it's short enough, leave as is
                           (str (str/join (take (- width 4) s)) "..." (last s))))]

    (if (<= (count email-address) (* 2 width))
      email-address
      (let [[_ id host] (re-matches #"(.*)@(.*)" email-address)]
        (str (crop-str id) "@" (crop-str host))))))
