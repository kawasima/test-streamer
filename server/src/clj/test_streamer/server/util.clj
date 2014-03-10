(ns test-streamer.server.util
  (:import [java.net Socket]))

(defn available-port
  ([start]
    (available-port start (+ start 30)))
  ([start end]
    (some (fn [p]
            (try
              (.close (Socket. "localhost" p))
              (catch Exception e p)))
      (range start (inc end))))) 
  
