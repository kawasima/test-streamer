(ns test-streamer.server.component.socketapp
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :refer [info debug warn]]
            [clojure.core.async :refer [put!]]
            [clojure.edn :as edn]))

(defn progress
  "Calculate a progress rate for a test shot."
  [entries shot-id]
  (let [shot (get @entries shot-id)
        prog (- 100 (float (/ (* 100 (count (filter nil? (vals (:results shot)))))
                             (count (vals (:results shot))))))]
    (swap! entries assoc-in [shot-id :progress] prog)
    prog))


(defmulti handle-command (fn [msg _ _]
                           (:command msg)))

(defmethod handle-command :ready
  ([msg ch {:keys [clients]}]
   (info "Ready client " (.hashCode ch))
   (swap! clients assoc ch
          (merge {:status :stand-by}
                 (dissoc msg :command)))))

(defmethod handle-command :bye
  ([msg ch {:keys [clients]}]
   (info "Disconnect client " (.hashCode ch))
   (swap! clients dissoc ch)))

(defmethod handle-command :result
  ([msg ch {:keys [clients shots-queue entries available-clients]}]
    (info "Test finished! " (:result msg))
    (if-let [client-exception (get-in msg [:result :client-exception])]
      (do
        ;; Push back when unexpected errors occurs in a client.
        (put! shots-queue (select-keys msg [:shot-id :name :classloader-id]))
        (swap! clients assoc-in [ch :status] :error))
      (do
        (swap! entries assoc-in [(str (:shot-id msg)) :results (:name msg)] (:result msg))
        (if (= (progress entries (str (:shot-id msg))) 100.0)
          (deliver (get-in @entries [(str (:shot-id msg)) :complete?]) true))
        (swap! clients assoc-in [ch :status] :stand-by)
        (deliver @available-clients true)))))

(defrecord SocketApp [dispatcher test-shots]
  component/Lifecycle

  (start [component]
    (let [options {:clients (:clients dispatcher)
                   :available-clients (:available-clients dispatcher)
                   :entries (:entries test-shots)
                   :shots-queue (:shots-queue test-shots)}]
      (assoc component
             :path "/join"
             :on-message (fn [ch message]
                           (handle-command (edn/read-string message) ch options))
             :on-close (fn [ch close-reason]
                         (info "disconnect" ch "for" close-reason)
                         (handle-command {:command :bye} ch options)))))

  (stop [component]
    (dissoc component :path :on-message :on-close)))

(defn socketapp-component [options]
  (map->SocketApp options))
