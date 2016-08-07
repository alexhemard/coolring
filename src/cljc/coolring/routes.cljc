(ns coolring.routes
  (:require [bidi.bidi :as bidi]))

(def routes
  ["/" [[""         :index]
        ["login"    {:get :login}]
        ["register"  {:get  :registration
                      :post :register}]
        [["sites/" [long :id]] {:post [["/approve"    :approve-site]
                                       ["/deactivate" :deactivate-site]]}]
        ["rings" [[""    {:post {"" :create-ring}
                          :get  {"" :rings}}]
                  ["/new" :new-ring]
                  [["/" [long :id]] [["" :ring]]]]]]
   true :not-found])
