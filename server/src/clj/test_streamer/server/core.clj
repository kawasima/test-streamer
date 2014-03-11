(ns test-streamer.server.core
  (:require [clojure.edn :as edn]
            [compojure.route :as route]
            [test-streamer.server.page :as page]
            [ring.util.response :as response])
  (:use [lamina.core]
        [aleph.http]
        [compojure.core :only [defroutes GET POST ANY]]
        [compojure.handler :only [site]]
        [clojure.tools.logging :only [info debug]]
        [ring.middleware.reload :only [wrap-reload]]
        [test-streamer.server.util :only [available-port]])
  (:import [net.unit8.wscl ClassProvider]
           [java.net InetAddress]))

(defonce clients (atom {}))
(defonce config (atom {}))
(defonce test-shots (atom {}))

(def test-queue (permanent-channel))

(defn find-stand-by []
  (some (fn [[ch opts]]
          (when (= (:status opts) :stand-by)
            ch))
    @clients))

(defn dispatcher []
  (while (not (Thread/interrupted)) 
    (let [test-request (edn/read-string @(read-channel test-queue))]
      (loop [ch (find-stand-by)]
        (if ch
          (do
            (info "Dispatch test (" (:name test-request) ") to " (.hashCode ch))
            (swap! clients assoc-in [ch :status] :busy)
            (enqueue ch (pr-str (assoc test-request :command :do-test))))
          (do
            (Thread/sleep 3000)
            (info "No available clients.")
            (recur (find-stand-by)))))))) 

(defn submit-tests [tests & opts]
  (let [shot-id (.toString (java.util.UUID/randomUUID))]
    (swap! test-shots assoc shot-id
      {:submitted-at (java.util.Date.)
       :results (apply hash-map (interleave tests (repeat nil)))})
    (doseq [test tests]
      (enqueue test-queue (pr-str {:shot-id shot-id :name test})))))

(defmulti handle :command)

(defmethod handle :class-provider-url
  ([msg ch]
    (enqueue ch (pr-str {:command :class-provider-url
                         :url (str "ws://" (.getHostAddress (InetAddress/getLocalHost))
                                ":" (:class-provider-port @config))}))))
(defmethod handle :ready
  ([msg ch]
    (info "Ready client " (.hashCode ch))
    (swap! clients assoc ch
      (merge {:status :stand-by}
        (dissoc msg :command)))
    (on-closed ch (fn []
                    (info "Disconnect client " (.hashCode ch))
                    (swap! clients dissoc ch)))))

(defmethod handle :result
  ([msg ch]
    (info "Test finished! " (:result msg))
    (swap! test-shots assoc-in [(:shot-id msg) :results (:name msg)] (:result msg))
    (swap! clients assoc-in [ch :status] :stand-by)))

(defn handler [ch request]
  (receive-all ch
    (fn [msg]
      (let [command (edn/read-string msg)]
        (handle command ch)))))

(defn- exists-class? [class-name]
  (and class-name
       (-> (Thread/currentThread)
           (.getContextClassLoader)
           (.getResource (str (.replace class-name \. \/) ".class")))))

(defroutes app-routes
  (GET "/join" []
    (wrap-websocket-handler handler))
  (ANY "/submit" {{test :test} :params :as request}
    (if (exists-class? test) 
      (do
        (submit-tests [test])
        (info "Submit test " test request)
        (response/redirect "/"))
      (str "Reject! " test " not found.")))
  (GET "/report/:shot-id" [shot-id]
    (page/report-page (get @test-shots shot-id)))
  (GET "/client" []
    (page/client-page))
  (GET "/" []
    (page/index-page :shots @test-shots :clients @clients))
  (route/resources "/")
  (route/not-found "Not Found"))

(defn stop []
  (when-let [svr-stop-fn (:test-provider @config)]
    (svr-stop-fn)
    (swap! config dissoc :test-provider)
  (when-let [svr (:class-provider @config)]
    (.stop svr)
    (swap! config dissoc :class-provider)))
  (when-let [dispatcher (:dispatcher @config)]
    (.interrupt dispatcher)))

(defn start [port]
  (let [port (Integer. (or port 5000))]
    (swap! config assoc
      :test-provider  (start-http-server
                        (wrap-ring-handler (wrap-reload (site app-routes)))
                        {:port port
                         :websocket true
                         :netty {:pipeline-transform #(doto % (.remove "deflater"))}})
      :class-provider (ClassProvider.)
      :dispatcher     (Thread. dispatcher))
    (.start (:dispatcher @config))
    (info "Started test provider (port=" port ")")
    (if-let [class-provider-port (available-port (inc port))]
      (do
        (swap! config assoc
          :class-provider-port class-provider-port)
        (.start (:class-provider @config) class-provider-port)
        (info "Started class provider (port=" class-provider-port ")"))
      (stop))))

(defn main-for-test [& args] (start 5050))

