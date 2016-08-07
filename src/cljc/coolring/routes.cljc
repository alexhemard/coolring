(ns coolring.routes
  (:require [bidi.bidi :as bidi]))

(def routes
  ["/" [[""         :index]
        ["login"    :login]
        ["register" :register]
        [["sites/" [long :id]] {:post [["/approve" :approve-site]]}]        
        ["rings" [[#"/?" {:get  {"" :rings}}]
                  [""    {:post {"" :create-ring}}]
                  ["/new" :new-ring]                  
                  [["/" [long :id]] [["" :ring]]]]]]
   true :not-found])
