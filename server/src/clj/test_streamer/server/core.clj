(ns test-streamer.server.core
  (:require [ring.util.servlet :as servlet]
            [clojure.edn :as edn]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults  site-defaults]]
            [test-streamer.server.page :as page]
            [test-streamer.server.output :as output]
            [test-streamer.server.test-shots :as test-shots]
            [ring.util.response :as response]
            [liberator.dev :as dev])
  (:use [clojure.core.async :only [go-loop <! put!]]
        [compojure.core :only [defroutes GET POST ANY]]
        [compojure.handler :only [site]]
        [clojure.tools.logging :only [info debug warn]]
        [ring.middleware.reload :only [wrap-reload]]
        [test-streamer.server.util :only [available-port]])
  (:import [java.net InetAddress]
           [net.unit8.wscl ClassProvider]
           [org.xnio ByteBufferSlicePool]
           [io.undertow Undertow Handlers]
           [io.undertow.servlet Servlets]
           [io.undertow.servlet.api DeploymentInfo]
           [io.undertow.servlet.util ImmediateInstanceFactory]
           [io.undertow.websockets WebSocketConnectionCallback]
           [io.undertow.websockets.core WebSockets AbstractReceiveListener]
           [io.undertow.websockets.jsr WebSocketDeploymentInfo]))

(defonce clients (atom {}))
(defonce config (atom {}))
(defonce available-clients (atom (promise)))


(defn find-stand-by
  "Find a stand-by client."
  []
  (some (fn [[ch opts]]
          (when (= (:status opts) :stand-by)
            ch))
    @clients))

(defn dispatcher []
  (go-loop []
    (let [test-request (<! test-shots/shots-queue)]
      (info "Dispatcher: processing a test-request: " test-request)
      (loop [ch (find-stand-by)]
        (if ch
          (do
            (info "Dispatch test (" (:name test-request) ") to " (.hashCode ch))
            (swap! clients assoc-in [ch :status] :busy)
            (WebSockets/sendText (pr-str (assoc test-request :command :do-test)) ch nil))
          (do
            (reset! available-clients (promise))
            (when-not (deref @available-clients 3000 false)
              (info "No available clients.")) 
            (recur (find-stand-by))))))))

(defmulti handle-command :command)

(defmethod handle-command :ready
  ([msg ch]
   (info "Ready client " (.hashCode ch))
   (swap! clients assoc ch
          (merge {:status :stand-by}
                 (dissoc msg :command)))))

(defmethod handle-command :bye
  ([msg ch]
   (info "Disconnect client " (.hashCode ch))
   (swap! clients dissoc ch)))

(defmethod handle-command :result
  ([msg ch]
    (info "Test finished! " (:result msg))
    (if-let [client-exception (get-in msg [:result :client-exception])]
      (do
        ;; Push back when unexpected errors occurs in a client.
        (put! test-shots/shots-queue (select-keys msg [:shot-id :name :classloader-id])) 
        (swap! clients assoc-in [ch :status] :error))
      (do
        (swap! test-shots/entries assoc-in [(str (:shot-id msg)) :results (:name msg)] (:result msg))
        (if (= (test-shots/progress (str (:shot-id msg))) 100.0)
          (deliver (get-in @test-shots/entries [(str (:shot-id msg)) :complete?]) true))
        (swap! clients assoc-in [ch :status] :stand-by)
        (deliver @available-clients true)))))

(defroutes app-routes
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
  (route/resources "/")
  (route/not-found "Not Found"))

(defn stop []
  (when-let [svr-stop-fn (:test-provider @config)]
    (svr-stop-fn)
    (swap! config dissoc :test-provider))
  (when-let [dispatcher (:dispatcher @config)]
    (.interrupt dispatcher)))

(defn websocket-callback [{:keys [on-close on-message]}]
  (proxy [WebSocketConnectionCallback] []
    (onConnect [exchange channel]
      (.. channel
          getReceiveSetter
          (set (proxy [AbstractReceiveListener] []
                 (onFullTextMessage
                   [channel message]
                   (when on-message (on-message channel (.getData message))))
                 (onCloseMessage
                   [message channel]
                   (when on-close (on-close channel message))))))
      (.resumeReceives channel)
      (.addCloseTask channel
                     (proxy [org.xnio.ChannelListener] []
                       (handleEvent [channel]
                         (warn "Client close: " channel)
                         (when on-close (on-close channel nil))))))))


(defn- websocket-classloader-provider []
  (.. (DeploymentInfo.)
      (setClassLoader (.getContextClassLoader (Thread/currentThread)))
      (setContextPath "")
      (addServletContextAttribute
       WebSocketDeploymentInfo/ATTRIBUTE_NAME
       (.. (WebSocketDeploymentInfo.)
           (setBuffers (ByteBufferSlicePool. (int 100) (int 1000)))
           (addEndpoint ClassProvider)))
      (setDeploymentName "WebSocketClassProvider")))

(defn run-server [ring-handler & {port :port websockets :websockets}]
  (let [ring-servlet (servlet/servlet ring-handler)
        servlet-builder (.. (Servlets/deployment)
                            (setClassLoader (.getContextClassLoader (Thread/currentThread)))
                            (setContextPath "")
                            (setDeploymentName "test-streamer")
                            (addServlets
                             (into-array
                              [(.. (Servlets/servlet "Ring handler"
                                                     (class ring-servlet)
                                                     (ImmediateInstanceFactory. ring-servlet))
                                   (addMapping "/*"))])))
        container (Servlets/defaultContainer)
        servlet-manager (.addDeployment container servlet-builder)
        wscl-manager    (.addDeployment container (websocket-classloader-provider))
        handler (Handlers/path)]
    ;; deploy
    (.deploy servlet-manager)
    (.deploy wscl-manager)

    (doseq [ws websockets]
      (.addPrefixPath handler
                      (:path ws)
                      (Handlers/websocket
                       (websocket-callback (dissoc ws :path))))) 
    (let [server (.. (Undertow/builder)
                     (addHttpListener port "0.0.0.0")
                     (setHandler (.addPrefixPath handler "/" (.start servlet-manager)))
                     (setHandler (.addPrefixPath handler "/wscl"  (.start wscl-manager)))
                     (build))]
      (.start server)
      server)))

(defn start [& {port :port}]
  (let [port (Integer. (or port 5000))]
    (swap! config assoc
      :test-provider  (run-server
                       (-> app-routes
                           (wrap-defaults site-defaults)
                           wrap-reload)
                       :port port
                       :websockets [{:path "/join"
                                     :on-message (fn [ch message]
                                                   (handle-command (edn/read-string message) ch))
                                     :on-close (fn [ch close-reason]
                                                 (info "disconnect" ch "for" close-reason)
                                                 (handle-command {:command :bye} ch))}])
      :dispatcher     (dispatcher))
    (info "Started test provider (port=" port ")")))

