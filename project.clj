(defproject net.thebendavis/scrolloflex "0.1.0-SNAPSHOT"
  :description "a simple scrollable, flexible rolodex webapp"

  :url "https://github.com/thebendavis/scrolloflex"

  :main net.thebendavis.handler
  :ring {:handler net.thebendavis.handler/handler
         :init    net.thebendavis.handler/init}

  :profiles {:uberjar {:aot :all}}

  :dependencies [[org.clojure/clojure "1.6.0"]

                 ;; for configuration
                 [environ "0.4.0"]

                 ;; web
                 [hiccup "1.0.5"]
                 [compojure "1.2.1"]
                 [ring "1.3.1"]]

  :plugins [[lein-ring "0.8.13"]
            [lein-marginalia "0.7.1"]])
