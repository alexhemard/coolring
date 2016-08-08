(ns coolring.handlers
  (:require [liberator.core     :refer [resource defresource]]
            [liberator.representation :as rep]
            [clojure.tools.logging :as log]
            [hiccup.page        :refer [html5 include-css include-js]]
            [hiccup.element     :refer [link-to image]]
            [hiccup.form        :refer [form-to] :as form]
            [ring.util.response :refer [content-type response]]
            [cheshire.core      :as json]
            [bidi.bidi          :refer [path-for] :as bidi]
            [ring.util.anti-forgery :as anti-forgery]
            [cemerick.friend             :as friend]
            [cemerick.friend.workflows   :refer [make-auth]]
            [cemerick.friend.credentials :refer [hash-bcrypt]]
            [coolring.routes    :refer [routes]]
            [coolring.query     :as query]
            [coolring.assets    :refer [js-asset css-asset img-asset]]))

(defn current-user [{:keys [db request] :as ctx}]
  (let [{:keys [identity]} (-> request
                             friend/identity
                             friend/current-authentication)
        user (query/user-by-id {:id identity} {:connection db
                                               :result-set-fn first})]
    (if user (assoc ctx :identity user) ctx)))

(defn initialize-context [context]
  (fn [ctx]
    (-> ctx
      (merge context)
      (current-user))))

(def authorization-required
  {:authorized? (fn [ctx] (contains? ctx :identity))
   :handle-unauthorized (fn [ctx]
                          (friend/throw-unauthorized
                            (friend/identity (get ctx :request))
                            {}))
   :allowed? (fn [ctx] (contains? ctx :identity))
   :handle-forbidden (fn [ctx]
                       (friend/throw-unauthorized
                         (friend/identity (get ctx :request))
                         {}))})

(defn nav [ctx]
  [:nav {:class "navigation"}
   [:a {:class "brand" :href "/"} "coolring.club"]
   (if-let [{:keys [email]} (:identity ctx)]
     [:ul {:class "navigation-list"}
      [:li {:class "navigation-item"} [:a {:href "/settings" :class "navigation-link"} email]]
      [:li {:class "navigation-item"}
       [:a {:class "navigation-button" :href "/rings/new"} "new ring"]]]
     [:ul {:class "navigation-list"}
      [:li {:class "navigation-item"} [:a {:class "navigation-link" :href "/login"} "login"]]
      [:li {:class "navigation-item"} [:a {:class "navigation-link" :href "/register"} "register"]]])])

(defn page [ctx title & body]
  (html5
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:title (str "coolring.club | " title)]
     (include-js  (js-asset  "app.js"))
     (include-css (css-asset "app.css"))
     [:body
      (nav ctx)
      [:div {:class "container"}
       body]]]))

(defmulti link (fn [type entity] type))

(defmethod link :default [type entity]
  (link-to {} (str "/" (name type) "/" (:id entity)) (:name entity)))

(defresource rings [ctx]
  authorization-required
  :initialize-context (initialize-context ctx)
  :available-media-types ["text/html"]
  :exists? (fn [{:keys [db identity]}]
             {:rings (query/rings-by-owner {:owner_id (:id identity)} {:connection db})})
  :handle-ok (fn [{:keys [rings] :as ctx}]
               (page ctx "home"
                 [:h2 "my web rings"]
                 [:ul
                  (for [ring rings]
                    [:li [:div
                          [:div (link :rings ring)]]])])))

(defresource ring [ctx]
  authorization-required
  :initialize-context (initialize-context ctx)
  :available-media-types ["text/html"]
  :exists? (fn [{:keys [db request] :as ctx}]
             (let [{:keys [route-params]} request
                   ring (query/ring-by-id route-params {:connection db
                                                        :result-set-fn first})
                   sites (query/approved-sites-for-ring ring {:connection db})]
               {:ring ring
                :sites sites}))
  :handle-ok (fn [{:keys [ring sites] :as ctx}]
               (page ctx (str (:name ring))
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
  :initialize-context (initialize-context ctx)
  :available-media-types ["text/html"]
  :allowed-methods [:post]
  :processable? (fn [{:keys [request identity] :as ctx}]
                  (let [{:keys [params]} request
                        ring (select-keys params [:name :description])
                        ring (assoc ring :owner_id (:id identity))]
                    {:ring ring}))
  :post! (fn [{:keys [db ring identity] :as ctx}]
           (let [ring (query/create-ring<! ring {:connection db})]
             {:id (:id ring)}))
  :post-redirect? (fn [{:keys [id]}]
                    {:location (path-for routes :ring :id id)}))

(defresource new-ring [ctx]
  authorization-required
  :initialize-context (initialize-context ctx)
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
               (page ctx "new web ring"
                 [:h2 "new ring"]
                 (form-to
                   {:class "ring-form"}
                   [:post "/rings"]
                   (anti-forgery/anti-forgery-field)
                   [:div {:class "ring-row"}
                    (form/label {:class "ring-label"} :name "name")
                    [:div {:class "ring-input-container"}
                     (form/text-field {:placeholder "webring name"
                                       :class "ring-input"} :name)]]
                   [:div {:class "ring-row"}
                    (form/label {:class "ring-label"} :description "description")
                    [:div {:class "ring-input-container"}
                     (form/text-field {:placeholder "description"
                                       :class "ring-input"} :description)]]
                   (form/submit-button {:class "submit-button"} "submit")))))

(defresource settings [ctx]
  :initialize-context (initialize-context ctx)
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
               (page ctx "settings"
                 [:h2 "settings"]
                 [:p "todo..."])))

(defresource new-site [ctx]
  :initialize-context (initialize-context ctx)
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
               (page ctx "new site"
                 [:h2 "todo"])))

(defresource approve-site [ctx]
  :authorization-required
  :initialize-context (initialize-context ctx)
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
  :initialize-context (initialize-context ctx)
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
  (page ctx "login"
    [:h2 "login"]
    (form-to
      {:class "login-form"}
      [:post "/login"]
      (anti-forgery/anti-forgery-field)
      [:div {:class "login-row"}
       (form/label {:class "login-label"} :email "email")
       [:div {:class "login-input-container"}
        (form/text-field {:placeholder "email"
                          :class "login-input"} :email)]]
      [:div {:class "login-row"}
       (form/label {:class "login-label"} :password "password")
       [:div {:class "login-input-container"}
        (form/password-field {:placeholder "password"
                              :class "login-input"} :password)]]
      (form/submit-button {:class "submit-button"} "submit"))))


(defresource login [ctx]
  :initialize-context (initialize-context ctx)
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx] (login-page ctx)))

(defn registration-page [ctx]
  (page ctx "register"
    [:h2 "register"]
    (form-to
      {:class "registration-form"}
      [:post "/register"]
      (anti-forgery/anti-forgery-field)
      [:div {:class "registration-row"}
       (form/label {:class "registration-label"} :email "email")
       [:div {:class "registration-input-container"}
        (form/text-field {:class "registration-input" :placeholder "email"} :email)]]
      [:div {:class "registration-row"}
       (form/label {:class "registration-label"} :password "password")
       [:div {:class "registration-input-container"}
        (form/password-field {:class "registration-input" :placeholder "password"} :password)]]
      [:div {:class "registration-row"}
       (form/label {:class "registration-label"} :repeat-password "repeat password")
       [:div {:class "registration-input-container"}
        (form/password-field {:class "registration-input" :placeholder "password"} :repeat-password)]]
      (form/submit-button {:class "submit-button"} "submit"))))

(defresource register [ctx]
  :initialize-context (initialize-context ctx)
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
                         {:connection db})
                 auth (make-auth {:identity (:id user) :email email})]
             {:auth auth}))
  :post-redirect? (fn [ctx]
                    {:location (path-for routes :rings)})
  :handle-see-other (fn [{:keys [auth request]}]
                      (rep/ring-response
                        (friend/merge-authentication {:status 303} auth)))
  :handle-conflict (fn [ctx] (registration-page ctx)))

(defresource registration [ctx]
  :initialize-context (initialize-context ctx)
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [{:keys [db] :as ctx}]
               (registration-page ctx)))

(defresource index [ctx]
  :initialize-context (initialize-context ctx)
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :exists? (fn [ctx] (not (contains? ctx :identity)))
  :existed? (fn [ctx] (contains? ctx :identity))
  :moved-temporarily? (fn [{:keys [identity]}]
                        (when identity
                          {:location (path-for routes :rings)}))
  :handle-ok (fn [{:keys [db identity] :as ctx}]
               (page ctx "welcome 2 coolring.club"
                 [:h2 "welcome 2 the future"]
                 [:p "create your own webring!"]
                 [:link {:rel "import" :href "cool.html"}]
                 [:p
                  (image {:class "homepage-image"} (img-asset "webring.gif") "webring")]
                 [:marquee [:p "\"wow. very cool. i am impressed!\" - internet magazine"]])))
