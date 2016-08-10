(ns coolring.core
  (:require [com.stuartsierra.component :refer [start-system start]]
            [coolring.system            :refer [system]]
            [coolring.util              :refer [parse-int]]
            [ragtime.repl                 :as ragtime]
            [ragtime.jdbc                 :as jdbc]
            [clojure.tools.cli          :refer [parse-opts]]
            [environ.core               :refer [env]])
  (:gen-class))

(def cli-options
  ;; An option with a required argument
  [["-p" "--port PORT" "Port number"
    :default (parse-int (env :port 8000))
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]])

(defn load-config []
  (let [db (start (:db (system {})))
        ds (jdbc/sql-database db {:migrations-table "migrations"})]
    {:datastore ds
     :migrations (jdbc/load-resources "migrations")}))

(defn migrate []
  (ragtime/migrate (load-config)))

(defn rollback []
  (ragtime/rollback (load-config)))

(defn -main [& args]
  (let [config {}
        {:keys [options arguments]} (parse-opts args cli-options)
        config (assoc-in config [:http :port] (:port options))]
    (condp = (first arguments)
      "migrate" (migrate)
      (start-system (system config)))))
