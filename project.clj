(defproject coolring "0.1.0-SNAPSHOT"
  :description "web rings 4 the cloud"

  :url "http://coolring.club"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :uberjar-name "coolring.jar"

  :dependencies [[bidi "2.0.9"]
                 [com.cemerick/friend "0.2.3"]
                 [com.stuartsierra/component "0.3.0"]
                 [cheshire "5.6.3"]
                 [crypto-password "0.1.3"]
                 [environ "1.0.1"]
                 [hiccup "1.0.5"]
                 [hikari-cp "1.7.2"]
                 [liberator "0.14.1"]
                 [log4j/log4j "1.2.17"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.12"]
                 [org.clojure/java.jdbc "0.6.2-alpha2"]
                 [org.postgresql/postgresql "9.4.1209"]
                 [ragtime "0.6.1"]
                 [ring-jetty-component "0.3.1"]
                 [ring/ring "1.5.0"]
                 [ring/ring-defaults "0.2.1"]
                 [yesql "0.5.3"]]

  :source-paths ["src/clj" "src/cljc" "src/sql"]

  :aliases {"gulp"        ["shell" "node_modules/gulp/bin/gulp.js" "build"]
            "npm-install" ["shell" "npm" "install"]}

  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]]
                   :source-paths ["dev"]}
             :uberjar {:aot :all
                       :prep-tasks ^:replace ["npm-install"
                                              "gulp"
                                              "javac"
                                              "compile"]}}

  :repl-options {:init-ns dev}

  :main coolring.core

  :plugins [[lein-environ "1.0.1"]
            [lein-shell "0.5.0"]])
