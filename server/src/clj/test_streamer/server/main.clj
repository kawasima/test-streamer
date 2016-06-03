(ns test-streamer.server.main
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [meta-merge.core :refer [meta-merge]]
            [test-streamer.server.config :as config]
            [test-streamer.server.system :refer [new-system]]))

(def prod-config {})

(def config
  (meta-merge config/defaults
              config/environ
              prod-config))

(defn -main [& args]
  (let [system (new-system config)]
    (component/start system)))

