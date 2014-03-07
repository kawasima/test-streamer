(ns test-streamer.examples.simple-server
  (:use [test-streamer.server :only [start stop]]))

(defn -main [& args]
  (start 5050))


