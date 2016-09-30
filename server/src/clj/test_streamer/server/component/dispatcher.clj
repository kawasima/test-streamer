(ns test-streamer.server.component.dispatcher
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [go-loop go timeout <! put! close!]]
            [clojure.tools.logging :refer [info debug warn]])
  (:import [io.undertow.websockets.core WebSockets]))

(defn find-stand-by
  "Find a stand-by client."
  [clients]
  (some (fn [[ch opts]]
          (when (= (:status opts) :stand-by)
            ch))
        @clients))

(defn dispatcher-loop [clients available-clients test-shots
                       & {:keys [timeout-ms] :or {timeout-ms 15000}}]
  (go-loop []
    (let [test-request (<! (:shots-queue test-shots))]
      (info "Dispatcher: processing a test-request: " test-request)
      (loop [ch (find-stand-by clients)]
        (if ch
          (do
            (info "Dispatch test (" (:name test-request) ") to " (.hashCode ch))
            (swap! clients assoc-in [ch :status] :busy)
            (WebSockets/sendText (pr-str (assoc test-request :command :do-test)) ch nil)
            (go (<! (timeout timeout-ms))
              (let [result (-> @(:entries test-shots)
                               (get-in [(str (:shot-id test-request)) :results (:name test-request)]))]
                (when-not result
                  (swap! (:entries test-shots) assoc-in
                         [(:shot-id test-request) :results (:name test-request)]
                         :timeout)
                  (put! (:shots-queue test-shots)
                        (update-in test-request [:retry-count] inc))))))
          (do
            (reset! available-clients (promise))
            (when-not (deref @available-clients 3000 false)
              (info "No available clients."))
            (recur (find-stand-by clients)))))
      (recur))))

(defrecord DispatcherComponent [test-shots]
  component/Lifecycle

  (start [component]
    (let [clients (atom {})
          available-clients (atom (promise))
          main-loop (dispatcher-loop clients available-clients test-shots)]
      (assoc component
             :clients clients
             :available-clients available-clients
             :main-loop main-loop)))

  (stop [component]
    (if-let [main-loop (:main-loop component)]
      (close! main-loop))
    (dissoc component :main-loop :clients :available-clients)))

(defn dispatcher-component [options]
  (map->DispatcherComponent options))
