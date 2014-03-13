(ns test-streamer.client.core
  (:require [clojure.edn :as edn]
            [test-streamer.client.ui :as ui])
  (:use [lamina.core]
        [aleph.http])
  (:gen-class)
  (:import [org.junit.runner JUnitCore]
           [java.net InetAddress]
           [net.unit8.wscl WebSocketClassLoader]
           [junit.framework AssertionFailedError]))

(defonce class-provider-url (atom nil))

(defn extract-stack-trace [^:Throwable t]
  (with-open [wtr (java.io.StringWriter.)]
    (.printStackTrace t (java.io.PrintWriter. wtr))
    (.toString wtr)))

(defn add-failure [results failure]
  (swap! results update-in [:failures] inc)
  (swap! results assoc-in [:testcases (dec (count (:testcases @results))) :failure]
    {:type (type (.getException failure))
     :message (.getMessage failure)
     :stacktrace (extract-stack-trace (.getException failure))}))

(defn add-error [results failure]
  (swap! results update-in [:errors] inc)
  (swap! results assoc-in [:testcases (dec (count (:testcases @results))) :error]
    {:type (type (.getException failure))
     :message (.getMessage failure)
     :stacktrace (extract-stack-trace (.getException failure))}))

(defn junit-core [results]
  (let [core (JUnitCore.)]
    (.addListener core
      (proxy
        [org.junit.runner.notification.RunListener] []
        (testRunStarted [description]
          (ui/running (-> (bean description) :children first)))
        
        (testStarted [description]
          (swap! results update-in [:testcases]
            #(conj % (-> (bean description)
                       (select-keys [:suite :test :className :methodName])
                       (assoc :time (System/currentTimeMillis))))))
        (testFailure [failure]
          (println (bean failure))
          (if (some #{AssertionError AssertionFailedError} [(type (.getException failure))])
            (add-failure results failure)
            (add-error results failure)))

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

(defn- client-spec []
  (let [mx (java.lang.management.ManagementFactory/getOperatingSystemMXBean)]
    {:os-name (.getName mx)
     :os-version (.getVersion mx)
     :cpu-arch (.getArch mx)
     :cpu-core (.getAvailableProcessors mx)}))

(defmulti handle :command)

(defmethod handle :class-provider-url [msg ch]
  (reset! class-provider-url (:url msg))
  (enqueue ch (pr-str (merge {:command :ready
                              :client-name (.getHostName (InetAddress/getLocalHost))}
                        (client-spec)))))

(defmethod handle :do-test [msg ch]
  (let [loader (WebSocketClassLoader. @class-provider-url)
        test-class (.loadClass loader (:name msg) true)
        test-classes (into-array Class [test-class])
        results (atom {:testcases [] :tests 0 :errors 0 :failures 0})
        original-loader (.getContextClassLoader (Thread/currentThread))]
    (.setContextClassLoader (Thread/currentThread) loader)
    (try
      (.run (junit-core results) test-classes)
      (catch Exception ex
        (swap! results assoc :client-exception (.getMessage ex)))
      (finally
        (.setContextClassLoader (Thread/currentThread) original-loader)
        (ui/standby)))
    (enqueue ch (pr-str {:command :result
                         :shot-id (:shot-id msg)
                         :name    (:name msg)
                         :result  @results}))))

(defmethod handle :benchmark [msg ch]
  (let [t1 (System/nanoTime)]
    (eval msg)
    (enqueue ch (pr-str (float (/ (- (System/nanoTime) t1) 1000000))))))

(defn connect [test-server-url]
  (let [c (atom nil)]
    (while (nil? @c)
      (try
        (reset! c (websocket-client {:url test-server-url}))
        (let [ch (wait-for-result @c)]
          (on-closed ch (fn []
                          (ui/disconnect)
                          (connect test-server-url)))
          (receive-all ch #(handle (edn/read-string %) ch))
          (ui/standby)
          (enqueue ch (pr-str {:command :class-provider-url})))
        (catch Exception e
          (println (.getMessage e))
          (reset! c nil)
          (Thread/sleep 5000))))))

(defn start [test-server-url]
  (ui/create-tray-icon)
  (connect test-server-url))

(defn -main [& args]
  #_(start "ws://labs.unit8.net:5050/join")
  (start "ws://localhost:5050/join"))
