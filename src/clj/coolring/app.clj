(ns coolring.app
  (:require [com.stuartsierra.component  :as component]
            [ring.middleware.defaults    :refer [wrap-defaults site-defaults]]
            [ring.util.request           :as req]
            [ring.util.response          :as resp]
            [ring.middleware.file        :refer [file-request]]
            [bidi.bidi                   :as bidi]
            [bidi.ring                   :refer [redirect make-handler resources-maybe]]
            [cemerick.friend             :as friend]
            [cemerick.friend.workflows   :refer [make-auth]]
            [cemerick.friend.credentials :refer [bcrypt-verify]]            
            [coolring.routes             :refer [routes]]
            [coolring.query              :as query]
            [coolring.handlers           :as h]))

(defn login-failed
  [request]
  (ring.util.response/redirect
    (str "/login" (str "?login_failed=Y"))))

(defn login-workflow
  [ctx]
  (let [{:keys [db]} ctx]
    (fn [{:keys [request-method params] :as request}]
      (when (and (= "/login" (req/path-info request))
                  (= :post request-method))
        (let [{:keys [email password]} params
              user (query/user-by-email {:email email} {:connection db
                                                        :result-set-fn first})
              {:keys [id email hashword]} user]
          (if (and user (bcrypt-verify password hashword))
            (cemerick.friend.workflows/make-auth
              {:identity id
               :email    email})
            (login-failed request)))))))

(defn index [req]
  (-> (resp/response "cool :)")
    (resp/content-type "text/html")))

(extend-protocol bidi.ring/Ring
  bidi.ring.ResourcesMaybe
  (request [resource req _]
    (or (file-request req (get-in resource [:options :prefix]))
        (resp/not-found "not found yall"))))

(defn handler-map [ctx]
  {:index           (h/index ctx)
   :login           (h/login ctx)
   :register        (h/register ctx)
   :registration    (h/registration ctx)   
   :rings           (h/rings ctx)
   :ring            (h/ring  ctx)
   :create-ring     (h/create-ring ctx)
   :new-ring        (h/new-ring    ctx)
   :approve-site    (h/approve-site ctx)
   :deactivate-site (h/deactivate-site ctx)
   :not-found   (resources-maybe {:prefix "resources/public"})})

(defn app [ctx]
  (-> routes
    (make-handler (handler-map ctx))
    (friend/authenticate {:workflows [(login-workflow ctx)]})
    (wrap-defaults site-defaults)))

(defrecord App [handler]
  component/Lifecycle
  (start [component]
    (assoc component :handler (app component)))
  (stop [component]
    component))
(defn create-app []
  (map->App {}))
