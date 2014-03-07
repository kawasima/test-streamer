(ns test-streamer.server
  (:require [clojure.edn :as edn])
  (:use [lamina.core]
        [aleph.http]
        [clojure.tools.logging :only [info debug]]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.keyword-params :only [wrap-keyword-params]])
  (:import [net.unit8.wscl ClassProvider]))

(defonce clients (atom {}))

(def broadcast-channel (permanent-channel))

(defn find-stand-by []
  (some (fn [[ch opts]]
          (when (= (:status opts) :stand-by)
            ch))
    @clients))

(defn dispatch [test]
  (loop [ch (find-stand-by)]
    (if ch
      (do
        (info "Dispatch test (" test ") to " (.hashCode ch))
        (swap! clients assoc-in [ch :status] :busy)
        (enqueue ch (pr-str {:command :do-test :name test})))
      (do
        (Thread/sleep 3000)
        (info "No available clients.")
        (recur (find-stand-by))))))

(defn submit-tests [tests & opts]
  (doall (map dispatch tests)))

(defmulti handle :command)

(defmethod handle :ready
  ([msg ch]
    (info "Ready client " (.hashCode ch))
    (swap! clients assoc-in [ch :status] :stand-by)))

(defmethod handle :result
  ([msg ch]
    (info "Test finished! " (:result msg))
    (swap! clients assoc-in [ch :status] :stand-by)))

(def handle-http
  (-> (fn [request]
        (case (:uri request)
          "/submit" (let [test (get-in request [:params :test])]
                      (submit-tests [test])
                      (info "Submit test " test request)
                      (enqueue (:channel request) {:status 200 :body "Accept!"}))
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

(defonce server (atom nil))

(defn start [port]
  (let [port (Integer. (or port 5050))]
    (debug "server starting...")
    (reset! server
            (start-http-server handler
                               {:port port
                                :websocket true}))
    (.start (ClassProvider.) 5000)
    (info "started")))

(defn stop []
  (when @server
    (@server)
    (reset! server nil)))

