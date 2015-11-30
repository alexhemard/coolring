(ns user
  (:require [clojure.java.io              :as io]
            [clojure.string               :as str]
            [clojure.pprint               :refer [pprint]]
            [clojure.repl                 :refer :all]
            [clojure.test                 :as test]
            [com.stuartsierra.component   :as component]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [coolring.system              :as coolring]
            [ragtime.repl                 :as ragtime]
            [ragtime.jdbc                 :as jdbc]))

(def system nil)

(def config {:http {:port 3332}})

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system
                  (constantly (coolring/system config))))

(defn start
  "Starts the current development system."
  []
  (alter-var-root #'system  component/start-system))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system component/stop-system))

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after `go))

(defn migrate []
  (let [db (jdbc/sql-database (:db system) {:migrations-table "migrations"})]
    (ragtime/migrate {:datastore db
                      :migrations (jdbc/load-resources "migrations")})))
