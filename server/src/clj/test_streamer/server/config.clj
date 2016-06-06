(ns test-streamer.server.config
  (:require [environ.core :refer [env]]))

(def defaults {:http {:port 5000
                      :prefix ""}})

(def environ
  {:http {:port (some-> env :port Integer.)}})

