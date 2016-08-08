(ns coolring.core
  (:require [com.stuartsierra.component :refer [start-system start]]
            [coolring.system            :refer [system]]
            [ragtime.repl                 :as ragtime]
            [ragtime.jdbc                 :as jdbc]
            [environ.core               :refer [env]])
  (:gen-class))

(def config {:http {:port (env :port 8000)}})

(defn load-config []
  (let [db (start (:db (system config)))
        ds (jdbc/sql-database db {:migrations-table "migrations"})]
    {:datastore ds
     :migrations (jdbc/load-resources "migrations")}))

(defn migrate []
  (ragtime/migrate (load-config)))

(defn rollback []
  (ragtime/rollback (load-config)))

(defn -main []
  (start-system (system config)))
