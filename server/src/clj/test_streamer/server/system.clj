(ns test-streamer.server.system
  (:require [com.stuartsierra.component :as component]
            [meta-merge.core :refer [meta-merge]]
            (test-streamer.server.component [undertow :refer [undertow-server]]
                                            [dispatcher :refer [dispatcher-component]]
                                            [test-shots :refer [test-shots-component]]
                                            [webapp :refer [webapp-component]]
                                            [socketapp :refer [socketapp-component]])))

(def base-config {})

(defn new-system [config]
  (let [config (meta-merge base-config config)]
    (-> (component/system-map
         :http (undertow-server (:http config))
         :dispatcher (dispatcher-component (:dispatcher config))
         :test-shots (test-shots-component (:test-shots config))
         :webapp (webapp-component (:webapp config))
         :socketapp (socketapp-component (:socketapp config)))
        (component/system-using
         {:http [:webapp :socketapp]
          :dispatcher [:test-shots]
          :webapp [:test-shots :dispatcher]
          :socketapp [:test-shots :dispatcher]}))))

