(defproject redial "0.1.0-SNAPSHOT"
  :description "A simple URL shortener."
  :url "https://github.com/ryankask/redial"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.1.8"]
                 [ring/ring-jetty-adapter  "1.1.8"]
                 [ring/ring-json "0.2.0"]
                 [org.clojure/java.jdbc "0.3.0-alpha4"]
                 [postgresql/postgresql "8.4-702.jdbc4"]
                 [clj-time "0.5.0"]]
  :plugins [[lein-ring "0.8.5"]]
  :ring {:handler redial.handler/app}
  :profiles {:dev {:dependencies [[ring-mock "0.1.3"]]}}
  :main redial.handler)
