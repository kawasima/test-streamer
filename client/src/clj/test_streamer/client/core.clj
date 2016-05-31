(ns test-streamer.client.core
  (:require [clojure.edn :as edn]
            [test-streamer.client.ui :as ui])
  ;(:use [clojure.core.async])
  (:gen-class)
  (:import [org.junit.runner JUnitCore]
           [java.net InetAddress URI]
           [java.security Policy Permissions AllPermission]
           [java.lang.reflect Modifier]
           [net.unit8.wscl WebSocketClassLoader]
           [junit.framework AssertionFailedError]
           [javax.websocket ContainerProvider Endpoint MessageHandler$Whole]))

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

(defmethod handle :class-provider-port [msg session]
  (swap! config assoc :class-provider-url
         (str "ws://" (:server-host @config) ":" (:port msg)))
  (.. session
      getAsyncRemote
      (sendText (pr-str
                 (merge {:command :ready
                         :client-name (.getHostName (InetAddress/getLocalHost))}
                        (client-spec))))))

(defmethod handle :do-test [msg session]
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
    (.. session getAsyncRemote
        (sendText (pr-str {:command :result
                           :shot-id (:shot-id msg)
                           :name    (:name msg)
                           :result  @results})))))

(defmethod handle :benchmark [msg session]
  (let [t1 (System/nanoTime)]
    (eval msg)
    (.. session getAsyncRemote
        (sendText (pr-str (float (/ (- (System/nanoTime) t1) 1000000)))))))

(defonce websocket-container (ContainerProvider/getWebSocketContainer))

(defn connect [test-server-url]
  (let [c (atom nil)]
    (while (nil? @c)
      (try
        (reset! c (.connectToServer websocket-container
                                    ^Endpoint (proxy [Endpoint] []
                                                (onOpen  [session config]
                                                  (.addMessageHandler session
                                                                      (reify MessageHandler$Whole
                                                                        (onMessage [this msg]
                                                                          (handle (edn/read-string msg) session)))))
                                                (onClose [session close-reason]
                                                  (ui/disconnect)
                                                  (connect test-server-url))
                                                (onError [session exception]))
                                    nil (URI/create test-server-url)))
        (ui/standby)
        (catch Exception e
          (println (.getMessage e))
          (reset! c nil)
          (Thread/sleep 5000))))))

(defn start [test-server-url]
  (ui/create-tray-icon)
  (let [session (connect test-server-url)]
    (swap! config assoc :server-host (.getHost (URI. test-server-url)))
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. #(when session (.close session))))))

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
