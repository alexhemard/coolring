(ns coolring.util)

(defn parse-int
  [s]
  (try
    (cond
     (string? s) (Integer/parseInt (re-find #"\A-?\d+" s))
     (number? s) s
     :else 0)
    (catch Exception e)))
