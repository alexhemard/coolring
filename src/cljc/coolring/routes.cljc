(ns coolring.routes
  (:require [bidi.bidi :as bidi]))

(def routes
  ["/" [[""         :index]
        ["settings" {:get :settings}]
        ["login"    {:get :login}]
        ["register"  {:get  :registration
                      :post :register}]
        [["sites/" [long :id]] {:post [["/approve"    :approve-site]
                                       ["/deactivate" :deactivate-site]]}]
        ["rings" [[#{"" "/"}    {:post {"" :create-ring}
                          :get  {"" :rings}}]
                  ["/new" :new-ring]
                  [["/" [long :id]] [["" :ring]
                                     ["/submit" {:get :submit-site}]
                                     ["/sites"  {:post :create-site}]
                                     [["/" [#".*" :url]] :site]]]]]]
   true :not-found])
