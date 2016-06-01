(ns test-streamer.server.system
  (:require [com.stuartsierra.component :as component]
            (test-streamer.server.component [undertow :refer [undertow-server]]
                                            [dispatcher :refer [dispatcher-component]]
                                            [test-shots :refer [test-shots-component]]
                                            [webapp :refer [webapp-component]]
                                            [socketapp :refer [socketapp-component]])))

(defn new-system [config]
  (-> (component/system-map
       :http (undertow-server (:http config))
       :dispatcher (dispatcher-component (:dispatcher config))
       :test-shots (test-shots-component (:test-shots config))
       :webapp (webapp-component)
       :socketapp (socketapp-component))
      (component/system-using
       {:http [:webapp :socketapp]
        :dispatcher [:test-shots]
        :webapp [:test-shots :dispatcher]
        :socketapp [:test-shots :dispatcher]})))

