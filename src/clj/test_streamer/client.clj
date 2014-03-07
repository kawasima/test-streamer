(ns test-streamer.client
  (:require [clojure.edn :as edn])
  (:use [lamina.core]
        [aleph.http]
        [clojure.tools.logging :only [info debug]])
  (:import [org.junit.runner JUnitCore]
           [java.net InetAddress]
           [net.unit8.wscl WebSocketClassLoader]))

(defonce class-provider-url (atom nil))

(defn extract-stack-trace [^:Throwable t]
  (with-open [wtr (java.io.StringWriter.)]
    (.printStackTrace t (java.io.PrintWriter. wtr))
    (.toString wtr)))

(defn junit-core [results]
  (let [core (JUnitCore.)]
    (.addListener core
      (proxy
        [org.junit.runner.notification.RunListener] []
        (testRunStarted [description])
        (testStarted [description]
          (swap! results update-in [:testcases]
            #(conj % (-> (bean description)
                       (select-keys [:suite :test :className :methodName])
                       (assoc :time (System/currentTimeMillis))))))
        (testFailure [failure]
          (swap! results update-in [:errors] inc)
          (swap! results assoc-in [:testcases (dec (count (:testcases @results))) :error]
            {:type (type (.getException failure))
             :message (.getMessage failure)
             :stacktrace (extract-stack-trace (.getException failure))}))

        (testAssumtionFailure [failure]
          (swap! results update-in [:failures] inc)
          (swap! results assoc-in [:testcases (dec (count (:testcases @results))) :failure]
            {:type (type (.getException failure))
             :message (.getMessage failure)
             :stacktrace (extract-stack-trace (.getException failure))}))

        (testIgnored [description]
          (swap! results update-in [:testcases (dec (count (:testcases @results))) :skip?] true))

        (testFinished [description]
          (swap! results update-in [:tests] inc)
          (swap! results update-in [:testcases (dec (count (:testcases @results))) :time]
            #(- (System/currentTimeMillis) %)))

        (testRunFinished [result]
          (swap! results assoc :time (.getRunTime result)))))
    core))

(defmulti handle :command)

(defmethod handle :class-provider-url [msg ch]
  (reset! class-provider-url (:url msg))
  (enqueue ch (pr-str {:command :ready
                       :client-name (.getHostName (InetAddress/getLocalHost))})))

(defmethod handle :do-test [msg ch]
  (let [loader (WebSocketClassLoader. @class-provider-url)
        test-class (.loadClass loader (:name msg) true)
        test-classes (into-array Class [test-class])
        results (atom {:testcases [] :tests 0 :errors 0 :failures 0})]
    (.run (junit-core results) test-classes)
    (enqueue ch (pr-str {:command :result
                         :shot-id (:shot-id msg)
                         :name    (:name msg)
                         :result  @results}))))


(defn connect [test-server-url]
  (let [c (atom nil)]
    (while (nil? @c)
      (try
        (reset! c (websocket-client {:url test-server-url}))
        (let [ch (wait-for-result @c)]
          (on-closed ch (fn [] (connect test-server-url)))
          (receive-all ch #(handle (edn/read-string %) ch))
          (enqueue ch (pr-str {:command :class-provider-url})))
        (catch Exception e
          (println (.getMessage e))
          (reset! c nil)
          (Thread/sleep 5000))))))

(defn start [test-server-url]
  (connect test-server-url))

(defn -main [& args]
  (start "ws://localhost:5050"))
