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
            [coolring.routes             :refer [routes]]
            [coolring.query              :as query]
            [coolring.handlers           :as h]
            [crypto.password.pbkdf2      :as password]))

(defn login-failed
  [{:keys [form-params params] :as request}]
  (ring.util.response/redirect
    (let [redirect-params (str "&login_failed=Y&email="
                            (java.net.URLEncoder/encode (:email form-params)))]
      (str "/login" redirect-params))))

(defn login-workflow
  [ctx]
  (let [{:keys [db]} ctx]
    (fn [{:keys [request-method params form-params] :as request}]
      (when (:and (= "/login" (req/path-info request))
                  (= :post request-method))
        (let [creds {:email    (get form-params    :email)
                     :password (get form-params :password)}
              {:keys [id email password]} creds
              {:keys [email hashword] :as user} (first (query/user-by-email {:email email} {:connection db}))]
          (if (password/check password hashword)
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
  {:index       index
   :login       (h/login ctx)
   :register    (h/login ctx)
   :user        (h/user  ctx)
   :rings       (h/rings ctx)
   :ring        (h/ring  ctx)
   :create-ring (h/create-ring ctx)
   :new-ring    (h/new-ring    ctx)
   :approve-site (h/approve-site ctx)
   :not-found   (resources-maybe {:prefix "resources/public"})})

(defn app [ctx]
  (-> routes
    (make-handler (handler-map ctx))
    ; (friend/authenticate {:workflows [(login-workflow ctx)]})
    (wrap-defaults site-defaults)))

(defrecord App [handler]
  component/Lifecycle
  (start [component]
    (assoc component :handler (app component)))
  (stop [component]
    component))
(defn create-app []
  (map->App {}))
