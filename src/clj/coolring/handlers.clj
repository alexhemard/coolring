(ns coolring.handlers
  (:require [liberator.core     :refer [resource defresource]]
            [hiccup.page        :refer [html5 include-css include-js]]
            [hiccup.element     :refer [link-to]]
            [hiccup.form        :as form]
            [ring.util.response :refer [content-type response]]
            [cheshire.core      :as json]
            [bidi.bidi          :refer [path-for] :as bidi]
            [ring.util.anti-forgery :as anti-forgery]
            [cemerick.friend             :as friend]
            [cemerick.friend.workflows   :refer [make-auth]]            
            [cemerick.friend.credentials :refer [hash-bcrypt]]
            [coolring.routes    :refer [routes]]
            [coolring.query     :as query]
            [coolring.assets    :refer [js-asset css-asset]]))

(defn current-user [{:keys [db request] :as ctx}]
  (let [{:keys [identity]} (-> request
                             friend/identity
                             friend/current-authentication)
        user (query/user-by-id {:id identity} {:connection db
                                               :result-set-fn first})]
    (when user {:identity user})))

(def authorization-required
  {:authorized? current-user
   :handle-unauthorized (fn [ctx]
                          (friend/throw-unauthorized
                            (friend/identity (get ctx :request))
                            {}))
   :allowed? current-user
   :handle-forbidden (fn [ctx]
                       (friend/throw-unauthorized
                         (friend/identity (get ctx :request))
                         {}))})

(defn form-to
  [[method action] & body]
  (if (contains? #{:head :get} method)
    (form/form-to [method action] body)
    (form/form-to [method action]
      (anti-forgery/anti-forgery-field)
      body)))

(defn page [title & body]
  (html5
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     
     [:title (str "coolring.club | " title)]
     (include-js  (js-asset  "app.js"))
     (include-css (css-asset "app.css"))
     [:body
      [:div {:class "container"}
       [:nav [:h1 "coolring.club"]]
       body]]]))

(defmulti link (fn [type entity] type))

(defmethod link :default [type entity]
  (link-to {} (str "/" (name type) "/" (:id entity)) (:name entity)))

(defresource rings [ctx]
  authorization-required
  :initialize-context ctx
  :available-media-types ["text/html"]
  :exists? (fn [{:keys [db]}]
             {:rings (query/rings {} {:connection db})})
  :handle-ok (fn [{:keys [rings]}]
               (page "home"
                 [:h2 "my rings"]
                 [:ul
                  (for [ring rings]
                    [:li [:div
                          [:div (link :rings ring)]
                          [:div (:description ring)]]])]
                 (link-to "/rings/new" "new ring")
                 )))

(defresource ring [ctx]
  authorization-required
  :initialize-context ctx
  :available-media-types ["text/html"]
  :exists? (fn [{:keys [db request] :as ctx}]
             (let [{:keys [route-params]} request
                   ring (query/ring-by-id route-params {:connection db
                                                        :result-set-fn first})
                   sites (query/approved-sites-for-ring ring {:connection db})]
               {:ring ring
                :sites sites}))
  :handle-ok (fn [{:keys [ring sites] :as ctx}]
               (page (str (:name ring))
                 [:h2 (:name ring)]
                 [:p (:description ring)]
                 [:h2 "sites"
                  [:ul
                   (for [site sites]
                     [:li
                      (link-to {:target "_blank"} (str (:url site)) (:name site))])]]
                 (link-to "/rings" "back"))))

(defresource create-ring [ctx]
  authorization-required
  :initialize-context ctx
  :available-media-types ["text/html"]
  :allowed-methods [:post]
  :processable? (fn [{:keys [request] :as ctx}]
                  (let [{:keys [params]} request
                        ring (select-keys params [:name :description])
                        ring (assoc ring :owner_id 1)]
                    {:ring ring}))
  :post! (fn [{:keys [db ring identity] :as ctx}]
           (let [ring (query/create-ring<! (assoc ring :owner_id (:id identity)) {:connection db})]
             {:id (:id ring)}))
  :post-redirect? (fn [{:keys [id]}]
                    {:location (path-for routes :ring :id id)}))

(defresource new-ring [ctx]
  authorization-required
  :initialize-context ctx
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
               (page "new web ring"
                 [:h2 "new ring"]
                 (form-to [:post "/rings"]
                   [:div {:class "form-group"}
                    (form/label :name "name")
                    (form/text-field {:placeholder "webring name"
                                      :class "form-control"} :name)]
                   [:div {:class "form-group"}
                    (form/label :description "description")
                    (form/text-field {:placeholder "description"
                                      :class "form-control"} :description)]
                   (form/submit-button {:class "submit-button"} "submit")))))

(defresource new-site [ctx]
  :initialize-context ctx)

(defresource approve-site [ctx]
  :authorization-required
  :initialize-context ctx
  :allowed-methods [:post]
  :available-media-types ["text/html"]
  :exists? (fn [{:keys [db request]}]
             (let [{:keys [route-params]} request]
               (when-let [site (query/site-by-id route-params {:connection db
                                                               :result-set-fn first})]
                 {:site site})))
  :post! (fn [{:keys [db site]}]
           (query/approve-site! {:id (:id site)} {:connection db}))
  :post-redirect? (fn [{:keys [site]}]
                    {:location (path-for routes :ring :id (:ring_id site))}))

(defresource deactivate-site [ctx]
  authorization-required
  :initialize-context ctx
  :allowed-methods [:post]
  :available-media-types ["text/html"]
  :exists? (fn [{:keys [db request]}]
             (let [{:keys [route-params]} request]
               (when-let [site (query/site-by-id route-params {:connection db
                                                               :result-set-fn first})]
                 {:site site})))
  :post! (fn [{:keys [db site]}]
           (query/deactivate-site! {:id (:id site)} {:connection db}))
  :post-redirect? (fn [{:keys [site]}]
                    {:location (path-for routes :ring :id (:ring_id site))}))

(defn login-page [ctx]
  (page "login"
    [:h2 "login"]
    (form-to [:post "/login"]
      [:div {:class "form-group row"}
       (form/label {:class "col-sm-2 col-form-label"} :email "email")
       [:div {:class "col-sm-10"}
        (form/text-field {:placeholder "email"
                          :class "form-control"} :email)]]
      [:div {:class "form-group row"}
       (form/label {:class "col-sm-2 col-form-label"} :password "password")
       [:div {:class "col-sm-10"}
        (form/password-field {:placeholder "password"
                              :class "form-control"} :password)]]
      (form/submit-button {:class "submit-button"} "submit"))))

(defresource login [ctx]
  :initialize-context ctx
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [_] (login-page ctx)))

(defn registration-page [ctx]
  (page "register"
    [:h2 "register"]
    (form-to [:post "/register"]
      [:div
       (form/label :email "email")
       (form/text-field {:placeholder "email"} :email)]
      [:div
       (form/label :password "password")
       (form/password-field {:placeholder "password"} :password)]
      [:div
       (form/label :repeat-password "repeat password")
       (form/password-field {:placeholder "password"} :repeat-password)]                   
      (form/submit-button "submit"))))

(defresource register [ctx]
  :initialize-context ctx
  :allowed-methods [:post]
  :available-media-types ["text/html"]
  :exists? (fn [{:keys [db request]}]
             (let [{:keys [params]} request
                   {:keys [email]} params
                   user (query/user-by-email {:email email} {:connection db
                                                             :result-set-fn first})]
               (when user {:user user})))
  :post-to-existing? (fn [_] false)
  :put-to-existing? (fn [_] true)  
  :conflict? (fn [_] true)  
  :post! (fn [{:keys [db request]}]
           (let [{:keys [email password]} (:params request) 
                 user  (query/create-user<! {:email email
                                             :hashword (hash-bcrypt password)}
                         {:connection db})]
             (when user (:id user))))
  :post-redirect? (fn [_]
                    {:location (path-for routes :rings)})
  :handle-conflict (fn [_] (registration-page ctx)))

(defresource registration [ctx]
  :initialize-context ctx
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [{:keys [db]}]
               (registration-page ctx)))

(defresource index [ctx]
  authorization-required
  :initialize-context ctx
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [{:keys [db identity] :as ctx}]
               "wan gwan"))
