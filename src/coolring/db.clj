(ns coolring.db
  (:require [com.stuartsierra.component :as component]
            [hikari-cp.core :refer [make-datasource close-datasource]]))

(def default-config
  {:auto-commit        true
   :read-only          false
   :connection-timeout 30000
   :idle-timeout       600000
   :max-lifetime       1800000
   :minimum-idle       10
   :maximum-pool-size  2
   :adapter            "postgresql"
   :username           "postgres"
   :database-name      "coolring"
   :server-name        "localhost"
   :port-number        5432})

(defrecord Database [config datasource]
  component/Lifecycle
  (start [component]
    (let [ds-config (merge config default-config)
          ds        (make-datasource ds-config)]
      (assoc component :datasource ds)))

  (stop [component]
    (close-datasource datasource)
    (assoc component :datasource nil)))

(defn create-database [config]
  (map->Database {:config config}))
