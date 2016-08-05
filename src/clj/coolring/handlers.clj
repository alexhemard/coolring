(ns coolring.handlers
  (:require [liberator.core :refer [resource defresource]]
            [cheshire.core  :as json]
            [coolring.query :as query]))

(defresource rings [ctx]
  :initialize-context ctx
  :available-media-types ["application/json"]
  :exists? (fn [{:keys [db]}]
             {:rings (query/rings {} {:connection db})})
  :handle-ok (fn [{:keys [rings]}]
               (json/generate-string rings)))

(defresource ring [ctx]
  :initialize-context ctx
  :exists? (fn [{:keys [db route-params]}]
             {:ring nil}))

(defresource ring-sites [ctx]
  :initialize-context ctx)

(defresource site [ctx]
  :initialize-context ctx)

(defresource users [ctx]
  :initialize-context ctx)

(defresource user [ctx]
  :initialize-context ctx)
