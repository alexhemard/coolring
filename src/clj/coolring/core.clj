(ns coolring.core
  (:require [com.stuartsierra.component :refer [start-system]]
            [coolring.system            :refer [system]])
  (:gen-class))

(def config {:http {:port 8000}})

(defn -main []
  (start-system (system config)))
