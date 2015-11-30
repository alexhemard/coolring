(defproject coolring "0.1.0-SNAPSHOT"
  :description "web rings 4 the cloud"

  :url "http://coolring.club"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[bidi "1.22.0"]
                 [com.cemerick/friend "0.2.1"]
                 [com.stuartsierra/component "0.3.0"]
                 [environ "1.0.1"]
                 [hiccup "1.0.5"]
                 [hikari-cp "1.4.0"]
                 [liberator "0.14.0"]
                 [org.clojure/clojure "1.7.0"]
                 [org.postgresql/postgresql "9.4-1206-jdbc42"]
                 [ragtime "0.5.2"]
                 [ring-jetty-component "0.3.0"]
                 [ring/ring "1.4.0"]
                 [ring/ring-defaults "0.1.5"]]

  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]]
                   :source-paths ["dev"]}}

  :plugins [[cider/cider-nrepl "0.9.1"]
            [lein-environ "1.0.1"]
            [lein-less "1.7.5"]])
