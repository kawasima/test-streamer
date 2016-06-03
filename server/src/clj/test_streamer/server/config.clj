(ns test-streamer.server.config
  (:require [environ.core :refer [env]]))

(def defaults
  ^:displace {:http {:port 5000}})

(def environ
  {:http {:port (some-> env :port Integer.)}})
