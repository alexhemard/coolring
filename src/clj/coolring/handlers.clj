(ns coolring.handlers
  (:require [liberator.core     :refer [resource defresource]]
            [liberator.representation :as rep]
            [clojure.tools.logging :as log]
            [clojure.string     :refer [join]]
            [hiccup.page        :refer [html5 include-css include-js]]
            [hiccup.element     :refer [link-to image]]
            [hiccup.form        :refer [form-to] :as form]
            [ring.util.response :refer [content-type response]]
            [cheshire.core      :as json]
            [bidi.bidi          :refer [path-for] :as bidi]
            [ring.util.anti-forgery :as anti-forgery]
            [jkkramer.verily :refer [validate]]
            [cemerick.friend    :as friend]
            [cemerick.friend.workflows   :refer [make-auth]]
            [cemerick.friend.credentials :refer [hash-bcrypt]]            
            [coolring.routes    :refer [routes]]
            [coolring.query     :as query]
            [coolring.assets    :refer [js-asset css-asset img-asset]]))

(def validate-registration
  [[:required [:email :password :confirmation]]
   [:email [:email]]
   [:equal [:password :confirmation]]])

(def validate-login
  [[:required [:email :password]]])

(def validate-ring
  [[:required [:name]]])

(def validate-site
  [[:required [:url :name]]
   [:url [:url]]])

(defn show-errors [ctx]
  (if-let [errors (:errors ctx)]
    [:div {:class "alert"}
     [:ul
      (for [{:keys [keys msg]} errors
            key keys]
        [:li [:strong (name key)] (str " " msg)])]]))

(defn validation-required
  ([validator]
   (validation-required
     validator
     (fn [{:keys [errors]}]
       (join "<br/>"
         (flatten
           (for [error errors]
             (for [key (:keys error)]
               (str (name key) " " (:msg error)))))))))
   ([validator error-fn]
    {:processable? (fn [ctx]
                     (let [params (get-in ctx [:request :params])
                           errors (validate params validator)]
                       (if (empty? errors)
                         true
                         [false {:errors errors}])))
     :handle-unprocessable-entity (fn [ctx]
                                    (error-fn ctx))}))

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

(defn layout [ctx title & body]
  (html5
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:title (str "coolring.club | " title)]
     (include-js  (js-asset  "app.js"))
     (include-css (css-asset "app.css"))
     [:body
      body]]))

(defn default-page [ctx title & body]
  (layout ctx title
    [:div {:class "app"}
     (nav ctx)
     [:div {:class "main"}
      body]]))

(defresource rings [ctx]
  authorization-required
  :initialize-context (initialize-context ctx)
  :available-media-types ["text/html"]
  :exists? (fn [{:keys [db identity]}]
             {:rings (query/rings-by-owner {:owner_id (:id identity)} {:connection db})})
  :handle-ok (fn [{:keys [rings] :as ctx}]
               (default-page ctx "home"
                 [:h2 "my web rings"]
                 [:div {:class "ring-list"}
                  (for [{:keys [id name]} rings]
                    (link-to {:class "ring-item" :data-ring-id id} (path-for routes :ring :id id)
                      [:span name] [:div {:class "ring-action"} "delete"]))])))

(defresource ring [ctx]
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
               (default-page ctx (str (:name ring))
                 [:h2 (:name ring)]
                 [:p (:description ring)]
                 [:h2 "sites"]
                 [:div {:class "site-list"}
                  (for [site sites]
                    (link-to {:class "site-item" :target "_blank"} (str "/rings/" (:id ring) "/" (:url site)) (:name site)))]
                 [:a {:class "submit-button" :href (str "/rings/" (:id ring) "/submit")} "add site"])))

(defn new-ring-page [ctx]
  (default-page ctx "new web ring"
    [:h2 "new ring"]
    (show-errors ctx)
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
      (form/submit-button {:class "submit-button"} "submit"))))

(defresource create-ring [ctx]
  (merge
    authorization-required
    (validation-required validate-ring new-ring-page))
  :initialize-context (initialize-context ctx)
  :available-media-types ["text/html"]
  :allowed-methods [:post]
  :post! (fn [{:keys [db identity request] :as ctx}]
           (let [params (get request :params)
                 ring (assoc params :owner_id (:id identity))
                 ring (query/create-ring<! ring {:connection db})]
             {:id (:id ring)}))
  :post-redirect? (fn [{:keys [id]}]
                    {:location (path-for routes :ring :id id)}))

(defresource new-ring [ctx]
  authorization-required
  :initialize-context (initialize-context ctx)
  :available-media-types ["text/html"]
  :handle-ok new-ring-page)

(defresource settings [ctx]
  :initialize-context (initialize-context ctx)
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
               (default-page ctx "settings"
                 [:h2 "settings"]
                 [:p "todo..."])))

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
  (default-page ctx "login"
    [:h2 "login"]
    (show-errors ctx)
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
  (default-page ctx "register"
    [:h2 "register"]
    (show-errors ctx)
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
       (form/label {:class "registration-label"} :repeat-password "confirm password")
       [:div {:class "registration-input-container"}
        (form/password-field {:class "registration-input" :placeholder "confirm password"} :confirmation)]]
      (form/submit-button {:class "submit-button"} "submit"))))

(defresource register [ctx]
  (validation-required validate-registration registration-page)
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
               (layout ctx "welcome 2 coolring.club"
                 [:div {:class "app"}
                  (nav ctx)
                  [:div {:class "container"}
                   [:h2 "welcome 2 the future"]
                   [:p "create your own webring!"]
                   [:link {:rel "import" :href "cool.html"}]
                   [:p
                    (image {:class "homepage-image"} (img-asset "webring.gif") "webring")]
                   [:marquee [:p "\"wow. very cool. i am impressed!\" - internet magazine"]]]])))

(defn new-site-page [ctx]
  (default-page ctx "submit site"
    (let [ring (:ring ctx)]
      [:h2 (:name ring)]
      [:h3 "submit site"]
      (show-errors ctx)
      (form-to
        {:class "site-form"}
        [:post (str "/rings/" (:id ring) "/sites")]
        (anti-forgery/anti-forgery-field)
        [:div {:class "site-row"}
         (form/label {:class "site-label"} :name "name")
         [:div {:class "site-input-container"}
          (form/text-field {:placeholder "name"
                            :class "site-input"} :name)]]
        [:div {:class "site-row"}
         (form/label {:class "site-label"} :url "url")
         [:div {:class "site-input-container"}
          (form/text-field {:placeholder "url"
                            :class "site-input"} :url)]]        
        (form/submit-button {:class "submit-button"} "submit")))))

(defresource create-site [ctx]
  (merge
    authorization-required
    (validation-required validate-site new-site-page))
  :initialize-context (initialize-context ctx)
  :allowed-methods [:post]
  :available-media-types ["text/html"]
  :exists? (fn [{:keys [db request]}]
             (let [{:keys [route-params]} request]
               (when-let [ring (query/ring-by-id route-params {:connection db
                                                               :result-set-fn first})]
                 [false {:ring ring}])))
  :post! (fn [{:keys [db ring identity request]}]
           (let [params   (:params request)
                 approved (= (:id identity) (:owner_id ring))
                 site (query/create-site<! (assoc params :ring_id (:id ring)
                                             :owner_id (:id identity)
                                             :approved approved) {:connection db})]
             (when site
               {:site site})))
  :post-redirect? (fn [{:keys [site]}]
                    {:location (path-for routes :ring :id (:ring_id site))}))

(defresource new-site [ctx]
  authorization-required
  :initialize-context (initialize-context ctx)
  :available-media-types ["text/html"]
  :allowed-methods [:get]
  :exists? (fn [{:keys [db request]}]
             (let [{:keys [route-params]} request]
               (when-let [ring (query/ring-by-id route-params {:connection db
                                                               :result-set-fn first})]
                 {:ring ring})))  
  :handle-ok new-site-page)

(defn explore-page [{:keys [ring site] :as ctx}]
  (layout ctx "explore"
    [:div {:class "explore-container"}
     [:nav {:class "ring-toolbar"}
      [:a {:class "toolbar-brand" :href "/"} "coolring.club"]
      [:div {:class "toolbar-main"}
       [:div {:class "toolbar-previous"} [:a {:id "previous" :class "toolbar-link" :href "http://coolguyradio.com"} "⬅"]]       
       [:div {:class "toolbar-status"}
        [:div {:class "toolbar-ring"} (:name ring)]
        [:marquee {:class "toolbar-current"} (:name site)]]
       [:div {:class "toolbar-next"} [:a {:id "next" :class "toolbar-link" :href "http://durstsans.com"} "➡"]]       
       ]]
     [:iframe {:id "ring-iframe" :class "ring-iframe" :src (:url site) :sandbox "" :security "restricted"}]]))

(defresource explore [ctx]
  :initialize-context (initialize-context ctx)
  :available-media-types ["text/html"]
  :allowed-methods [:get]
  :exists? (fn [{:keys [db request]}]
             (let [{:keys [route-params]} request
                   ring (query/ring-by-id
                          route-params
                          {:connection db
                           :result-set-fn first})
                   _ (log/info route-params)
                   site (query/site-by-url {:ring_id (:id ring)
                                            :url (:url route-params)}
                          {:connection db
                           :result-set-fn first})]
               (if site
                 {:ring ring
                  :site site}
                 [false {:ring ring}])))
  :handle-ok explore-page)
