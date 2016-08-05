(ns coolring.assets
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]))

(def manifest
  (if-let [manifest (io/resource "public/assets/manifest.json")]
    (json/parse-string (slurp manifest))
    {}))

(defn- regex-file-seq
  "Lazily filter a directory based on a regex."
  [re dir]
  (filter #(re-find re (.getPath %)) (file-seq dir)))

(defn- split-ext
  "Returns a vector of `[name extension]`."
  [file]
  (let [base (.getName file)
        i (.lastIndexOf base ".")]
    (if (pos? i)
      [(subs base 0 i) (subs base i)]
      [base nil])))

(defn asset [path]
  (when-let [file (first (regex-file-seq
                     (re-pattern (str ".*" path "$"))
                     (io/file "resources/public/assets")))]
    (let [hash       (manifest (.getPath file))
          parent     (.getParent file)
          [name ext] (split-ext file)
          file (io/file (str parent "/" name "-" hash ext))]
      (when (.exists file)
        (clojure.string/replace (.getPath file) #"resources/public" "")))))

(defn css-asset [path]
  (let [path (str "/css/" path)]
       (or (asset path) path)))

(defn js-asset [path]
  (let [path (str "/js/" path)]
       (or (asset path) path)))
