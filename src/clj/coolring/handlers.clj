(ns coolring.handlers
  (:require [liberator.core     :refer [resource defresource]]
            [hiccup.page        :refer [html5 include-css include-js]]
            [hiccup.element     :refer [link-to]]
            [hiccup.form        :as form]
            [ring.util.response :refer [content-type response]]
            [cheshire.core      :as json]
            [bidi.bidi          :refer [path-for] :as bidi]
            [ring.util.anti-forgery :as anti-forgery]
            [coolring.routes    :refer [routes]]
            [coolring.query     :as query]
            [coolring.assets    :refer [js-asset css-asset]]))

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
     [:title (str "coolring.club | " title)]
     (include-js  (js-asset  "app.js"))
     (include-css (css-asset "app.css"))
     [:body
      [:div {:class "container"}
       body]]]))

(defmulti link (fn [type entity] type))

(defmethod link :default [type entity]
  (link-to {} (str "/" (name type) "/" (:id entity)) (:name entity)))

(defresource rings [ctx]
  :initialize-context ctx
  :available-media-types ["text/html"]
  :exists? (fn [{:keys [db]}]
             {:rings (query/rings {} {:connection db})})
  :handle-ok (fn [{:keys [rings]}]
               (page "home"
                 [:h1 "coolring.club"]
                 (link-to "/rings/new" "new ring")
                 [:ul
                  (for [ring rings]
                    [:li [:div
                          [:div (link :rings ring)]
                          [:div (:description ring)]]])])))

(defresource ring [ctx]
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
                 [:h1 (:name ring)]
                 [:h2 "sites"
                  [:ul
                   (for [site sites]
                     [:li
                      (link-to {:target "_blank"} (str (:url site)) (:name site))])]]
                 (link-to "/rings" "back"))))

(defresource create-ring [ctx]
  :initialize-context ctx
  :available-media-types ["text/html"]
  :allowed-methods [:post]
  :processable? (fn [{:keys [request] :as ctx}]
                  (let [{:keys [params]} request
                        ring (select-keys params [:name :description])
                        ring (assoc ring :owner_id 1)]
                    {:ring ring}))
  :post! (fn [{:keys [db ring] :as ctx}]
           (let [ring (query/create-ring<! ring {:connection db})]
             {:id (:id ring)}))
  :post-redirect? (fn [{:keys [id]}]
                    {:location (path-for routes :ring :id id)}))

(defresource new-ring [ctx]
  :initialize-context ctx
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
               (page "new web ring"
                 [:h1 "new ring"]
                 (form-to [:post "/rings"]
                   [:div
                    (form/label :name "name")
                    (form/text-field {:placeholder "webring name"} :name)]
                   [:div
                    (form/label :description "description")
                    (form/text-field {:placeholder "description"} :description)]
                   (form/submit-button "submit")))))

(defresource new-site [ctx]
  :initialize-context ctx)

(defresource approve-site [ctx]
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

(defresource users [ctx]
  :initialize-context ctx)

(defresource user [ctx]
  :initialize-context ctx)

(defresource login [ctx]
  :initialize-context ctx
  :handle-ok (fn [ctx]

               ))

(defresource register [ctx]
  :initialize-context ctx
  :allowed-methods [:post :get])
