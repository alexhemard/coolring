(ns coolring.system
  (:require [com.stuartsierra.component :as     component]
            [ring.component.jetty       :refer [jetty-server]]
            [coolring.app               :refer [create-app]]
            [coolring.db                :refer [create-database]]))

(defn system [config]
  (let [http-config (:http config)
        db-config   (:db   config)]
    (-> (component/system-map
          :config   config
          :app     (create-app)
          :db      (create-database db-config)
          :http    (jetty-server http-config))
      (component/system-using
        {:app    [:db]
         :http   [:app]}))))
