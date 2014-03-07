(ns test-streamer.server
  (:require [clojure.edn :as edn]
            [test-streamer.page :as page])
  (:use [lamina.core]
        [aleph.http]
        [clojure.tools.logging :only [info debug]]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.keyword-params :only [wrap-keyword-params]]
        [test-streamer.util :only [available-port]])
  (:import [net.unit8.wscl ClassProvider]
           [java.net InetAddress]))

(defonce clients (atom {}))
(defonce config (atom {}))
(defonce test-shots (atom []))

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
    (swap! test-shots conj {:shot-id shot-id
                            :submitted-at (java.util.Date.)
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
    (swap! clients assoc-in [ch :status] :stand-by)
    (on-closed ch (fn []
                    (info "Disconnect client " (.hashCode ch))
                    (swap! clients dissoc ch)))))

(defmethod handle :result
  ([msg ch]
    (info "Test finished! " (:result msg))
    (swap! test-shots assoc-in [(:shot-id msg) :results (:name msg)] (:result msg))
    (swap! clients assoc-in [ch :status] :stand-by)))

(def handle-http
  (-> (fn [request]
        (case (:uri request)
          "/submit" (let [test (get-in request [:params :test])]
                      (submit-tests [test])
                      (info "Submit test " test request)
                      (enqueue (:channel request) {:status 200 :body "Accept!"}))
          "/" (enqueue (:channel request) {:status 200 :body (page/index-page @test-shots)})
          (enqueue (:channel request) {:status 404 :body "404 Not Found"})))
    (wrap-keyword-params)
    (wrap-params)))

(defn handler [ch request]
  (cond
    (:websocket request) (receive-all ch
                           (fn [msg]
                             (let [command (edn/read-string msg)]
                               (handle command ch))))
    (= (:scheme request) :http) (handle-http (assoc request :channel ch))))

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
      :test-provider  (start-http-server handler
                        {:port port
                         :websocket true})
      :class-provider (ClassProvider.)
      :dispatcher     (Thread. dispatcher))
    (.start (:dispatcher @config))
    (info "Started test provider (port=" port ")")
    (if-let [class-provider-port (available-port (inc port))]
      (do
        (swap! config assoc
          :class-provider-port class-provider-port)
        (.start (:class-provider @config) class-provider-port)
        (info "Started class provider (port=" class-provider-port ")")
        (receive test-queue #()))
      (stop))))


