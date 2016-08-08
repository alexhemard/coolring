(ns coolring.db
  (:require [com.stuartsierra.component :as component]
            [hikari-cp.core :refer [make-datasource close-datasource]]
            [clojure.string :as string]
            [environ.core   :refer [env]])
   (:import [java.net URI URLEncoder]))

(def scheme-map {"postgres" "postgresql"})

(defn- parse-properties-uri [^URI uri]
  (let [host   (.getHost uri)
        port   (if (pos? (.getPort uri)) (.getPort uri))
        path   (.getPath uri)
        scheme (.getScheme uri)
        scheme (get scheme-map scheme scheme)]
    (merge
      {:server-name   host
       :database-name (string/replace path #"^/" "")
       :port-number   port
       :adapter       scheme}
     (if-let [user-info (.getUserInfo uri)]
       {:username (first (string/split user-info #":"))
        :password (second (string/split user-info #":"))}))))

(def default-config
  {:auto-commit        true
   :read-only          false
   :connection-timeout 2000
   :idle-timeout       600000
   :max-lifetime       1800000
   :minimum-idle       10
   :pool-name          "coolring-pool"
   :maximum-pool-size  6})

(defn db-spec []
  (-> (URI. (env :database-url
              "postgresql://postgres:postgres@localhost:5432/coolring"))
    (parse-properties-uri)
    (merge default-config)))

(defrecord Database [config datasource]
  component/Lifecycle
  (start [component]
    (let [ds-config (merge config (db-spec))
          ds        (make-datasource ds-config)]
      (assoc component :datasource ds)))

  (stop [component]
    (if datasource
      (close-datasource datasource))
    (assoc component :datasource nil)))

(defn create-database [config]
  (map->Database {:config config}))
