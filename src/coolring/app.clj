(ns coolring.app
  (:require [com.stuartsierra.component :as component]
            [ring.middleware.defaults   :refer [wrap-defaults site-defaults]]))

(defn app [ctx]
  (-> (fn [req] {})
    (wrap-defaults site-defaults)))

(defrecord App [handler]
  component/Lifecycle
  (start [component]
    (assoc component :handler (app component)))
  (stop [component]
    component))

(defn create-app []
  (map->App {}))
