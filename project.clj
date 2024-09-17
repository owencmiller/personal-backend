(defproject game-backend "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.2"]
                 [aleph "0.6.0"]
                 [manifold "0.3.0"]
                 [jumblerg/ring-cors "3.0.0"]]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler game-backend.handler/app}
  :main game-backend.handler/-main
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})
