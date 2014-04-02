(ns test-streamer.client.core
  (:require [clojure.edn :as edn]
            [test-streamer.client.ui :as ui])
  (:use [lamina.core]
        [aleph.http])
  (:gen-class)
  (:import [org.junit.runner JUnitCore]
           [java.net InetAddress URI]
           [java.security Policy Permissions AllPermission]
           [java.lang.reflect Modifier]
           [net.unit8.wscl WebSocketClassLoader]
           [junit.framework AssertionFailedError]))

(def config (atom {}))
(def loaders (atom {}))

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
          (let [desc (bean description)]
            (swap! results update-in [:testcases]
              #(conj % (-> desc
                         (select-keys [:suite :test])
                         (assoc
                           :classname (:className desc)
                           :name (:methodName desc)
                           :time (System/currentTimeMillis)))))))

        (testFailure [failure]
          (cond
            (some #{AssertionError AssertionFailedError} [(type (.getException failure))])
            (add-failure results failure)

            (Modifier/isAbstract (some-> (.getDescription failure) (.getTestClass) (.getModifires)))
            (do
              (swap! results update-in [:skipped] inc)
              (swap! results update-in [:testcases (dec (count (:testcases @results))) :skip?] true))

            :default (add-error results failure)))

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
            #(float (/ (- (System/currentTimeMillis) %) 1000))))

        (testRunFinished [result]
          (swap! results assoc :time (float (/ (.getRunTime result) 1000))))))
    core))

(defn- client-spec []
  (let [mx (java.lang.management.ManagementFactory/getOperatingSystemMXBean)]
    {:os-name (.getName mx)
     :os-version (.getVersion mx)
     :cpu-arch (.getArch mx)
     :cpu-core (.getAvailableProcessors mx)}))

(defmulti handle :command)

(defmethod handle :class-provider-port [msg ch]
  (swap! config assoc :class-provider-url
         (str "ws://" (:server-host @config) ":" (:port msg)))
  (enqueue ch (pr-str (merge {:command :ready
                             :client-name (.getHostName (InetAddress/getLocalHost))}
                        (client-spec)))))

(defmethod handle :do-test [msg ch]
  (let [results (atom {:name (:name msg) :testcases [] :tests 0 :errors 0 :failures 0 :skipped 0})
        original-loader (.getContextClassLoader (Thread/currentThread))]
    (try
      (let [url (str (:class-provider-url @config)
                     "?classLoaderId="
                     (:classloader-id msg))
            t1 (System/currentTimeMillis)
            loader (or (get @loaders (:classloader-id msg))
                       (WebSocketClassLoader. url))
            test-class (.loadClass loader (:name msg) true)]
        (.setContextClassLoader (Thread/currentThread) loader)
        (.run (junit-core results) (into-array Class [test-class]))
        (swap! loaders assoc (:classloader-id msg) loader))
      (catch Exception ex
        (.printStackTrace ex)
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
          (enqueue ch (pr-str {:command :class-provider-port}))
          ch)
        (catch Exception e
          (println (.getMessage e))
          (reset! c nil)
          (Thread/sleep 5000))))))

(defn start [test-server-url]
  (ui/create-tray-icon)
  (let [conn (connect test-server-url)]
    (swap! config assoc :server-host (.getHost (URI. test-server-url)))
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. #(when conn (close conn))))))

(defn -main [& [server-url]]
  (System/setSecurityManager nil)
  (Policy/setPolicy
   (proxy [Policy][]
     (getPermissions [codesource]
                     (let [perms (Permissions.)]
                       (.add perms (AllPermission.))
                       perms))
     (refresh [])))
  (start (str (or server-url "ws://localhost:5000") "/join")))
