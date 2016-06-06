(ns test-streamer.server.component.undertow
  (:require [com.stuartsierra.component :as component]
            [ring.util.servlet :as servlet]
            [hiccup.middleware :refer [wrap-base-url]]
            [hiccup.util :refer [url]]
            [compojure.core :refer [context]]
            [clojure.tools.logging :refer [info debug warn]])
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

(defn run-server [ring-handler & {:keys [port websockets prefix]}]
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
                      (str prefix (:path ws))
                      (Handlers/websocket
                       (websocket-callback (dissoc ws :path)))))
    (let [server (.. (Undertow/builder)
                     (addHttpListener port "0.0.0.0")
                     (setHandler (.addPrefixPath handler (str prefix "/")
                                                 (.start servlet-manager)))
                     (setHandler (.addPrefixPath handler (str prefix "/wscl")
                                                 (.start wscl-manager)))
                     (build))]
      (.start server)
      server)))

(defrecord UndertowServer [webapp socketapp port prefix]
  component/Lifecycle

  (start [component]
    (let [server (run-server (context prefix []
                                 (wrap-base-url (:handler webapp) prefix))
                             :prefix prefix
                             :port port
                             :websockets [socketapp])]
      (assoc component :server server)))

  (stop [component]
    (if-let [server (:server component)]
      (.stop (:server component)))
    (dissoc component :server)))

(defn undertow-server [options]
  (map->UndertowServer (merge {:port 5000} options)))
