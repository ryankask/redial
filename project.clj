(defproject redial "0.1.0-SNAPSHOT"
  :description "A simple URL shortener."
  :url "https://github.com/ryankask/redial"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [ring/ring-core "1.1.8"]
                 [ring/ring-jetty-adapter  "1.1.8"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [postgresql/postgresql "8.4-702.jdbc4"]
                 [clj-time "0.4.5"]]
  :plugins [[lein-ring "0.8.2"]]
  :ring {:handler redial.handler/app}
  :profiles {:dev {:dependencies [[ring-mock "0.1.3"]]}}
  :main redial.handler)
