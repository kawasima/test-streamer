(ns test-streamer.server.core
  (:require [clojure.edn :as edn]
            [compojure.route :as route]
            [test-streamer.server.page :as page]
            [test-streamer.server.output :as output]
            [test-streamer.server.test-shots :as test-shots]
            [ring.util.response :as response]
            [liberator.dev :as dev])
  (:use [lamina.core]
        [aleph.http]
        [compojure.core :only [defroutes GET POST ANY]]
        [compojure.handler :only [site]]
        [clojure.tools.logging :only [info debug]]
        [ring.middleware.reload :only [wrap-reload]]
        [test-streamer.server.util :only [available-port]])
  (:import [java.net InetAddress]))

(defonce clients (atom {}))
(defonce config (atom {}))


(defn find-stand-by []
  (some (fn [[ch opts]]
          (when (= (:status opts) :stand-by)
            ch))
    @clients))

(defn dispatcher []
  (try
    (while (not (Thread/interrupted))
      (let [test-request (edn/read-string @(read-channel test-shots/shots-queue))]
        (info "Dispatcher: processing a test-request: " test-request)
        (loop [ch (find-stand-by)]
          (if ch
            (do
              (info "Dispatch test (" (:name test-request) ") to " (.hashCode ch))
              (swap! clients assoc-in [ch :status] :busy)
              (enqueue ch (pr-str (assoc test-request :command :do-test))))
            (do
              (Thread/sleep 3000)
              (info "No available clients.")
              (recur (find-stand-by)))))))
    (catch Exception ex (.printStackTrace ex))))

(defmulti handle :command)

(defmethod handle :class-provider-port
  ([msg ch]
    (enqueue ch (pr-str {:command :class-provider-port
                         :port (:class-provider-port @config)}))))

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
    (if-let [client-exception (get-in msg [:result :client-exception])]
      (do
        (enqueue test-shots/shots-queue (pr-str (select-keys msg [:shot-id :name]))) ;; Push back when unexpected errors occurs in a client.
        (swap! clients assoc-in [ch :status] :error))
      (do
        (swap! test-shots/entries assoc-in [(.toString (:shot-id msg)) :results (:name msg)] (:result msg))
        ;; (if (test-shots/progress  (:shot-id msg))
        ;;   (enqueue ))
        (swap! clients assoc-in [ch :status] :stand-by)))))

(defn handler [ch request]
  (receive-all ch
    (fn [msg]
      (let [command (edn/read-string msg)]
        (handle command ch)))))


(defroutes app-routes
  (GET "/join" []
    (wrap-websocket-handler handler))
  (ANY "/test-shots" [] test-shots/list-test-shots)
  (ANY "/test-shots/:shot-id" [shot-id]
    (test-shots/entry-test-shot shot-id))
  (GET "/test-shots/:shot-id/report" [shot-id]
    (test-shots/entry-test-shot-report shot-id))
  (GET "/client" []
    (page/client-page))
  (GET "/test-streamer-client.jnlp" [:as request]
    (output/jnlp request))
  (GET "/client.jar" [:as request]
    (output/client-jar))
  (GET "/client.jar.pack.gz" [:as request]
    (output/client-jar :compress true))
  (GET "/" []
    (page/index-page :shots @test-shots/entries :clients @clients))
  (route/resources "/"))

(defn stop []
  (when-let [svr-stop-fn (:test-provider @config)]
    (svr-stop-fn)
    (swap! config dissoc :test-provider))
  (when-let [svr test-shots/class-provider]
    (.stop svr))
  (when-let [dispatcher (:dispatcher @config)]
    (.interrupt dispatcher)))

(defn start [port]
  (let [port (Integer. (or port 5000))]
    (swap! config assoc
      :test-provider  (start-http-server
                        (wrap-ring-handler (-> app-routes
                                               (dev/wrap-trace :header)
                                               site
                                               wrap-reload))
                        {:port port
                         :websocket true
                         :netty {:pipeline-transform #(doto % (.remove "deflater"))}})
      :dispatcher     (Thread. dispatcher))
    (.start (:dispatcher @config))
    (info "Started test provider (port=" port ")")
    (if-let [class-provider-port (available-port (inc port))]
      (do
        (swap! config assoc
          :class-provider-port class-provider-port)
        (.start test-shots/class-provider class-provider-port)
        (.addShutdownHook (Runtime/getRuntime)
                          (Thread. (fn [] (stop)
                                     (info "Stop test streamer server. See you next time!"))))
        (info "Started class provider (port=" class-provider-port ")"))
      (stop))))

(defn main-for-test [& args] (start 5050))

