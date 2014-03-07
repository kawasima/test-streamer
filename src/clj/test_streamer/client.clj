(ns test-streamer.client
  (:require [clojure.edn :as edn])
  (:use [lamina.core]
        [aleph.http]
        [clojure.tools.logging :only [info debug]])
  (:import [org.junit.runner JUnitCore]
           [net.unit8.wscl WebSocketClassLoader]))

(defn format-failure [failure]
  (-> (bean failure)
    (update-in [:exception] type)
    (dissoc :description)))

(defmulti handle :command)

(defmethod handle :do-test [msg ch]
  (let [ loader (WebSocketClassLoader. "ws://localhost:5000")
         test-class (.loadClass loader (msg :name) true)
         test-classes (into-array Class [test-class])
         result (JUnitCore/runClasses test-classes)]
    (enqueue ch (pr-str {:command :result
                         :result  (-> (bean result)
                                    (update-in [:failures] #(map format-failure %)))}))))

(defn start []
  (let [c (websocket-client {:url "ws://localhost:5050"})
        ch (wait-for-result c)]
    (receive-all ch #(handle (edn/read-string %) ch))
    (enqueue ch (pr-str {:command :ready}))))

(defn -main [& args]
  (start))
