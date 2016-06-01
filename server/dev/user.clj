(ns user
  (:refer-clojure :exclude [test])
  (:require  [clojure.repl :refer :all]
             [clojure.pprint :refer [pprint]]
             [clojure.tools.namespace.repl :refer [refresh]]
             [reloaded.repl :refer [system init start stop go reset]]
             [test-streamer.server.system :as system]))

(defn new-system []
  (into (system/new-system {})
        {}))

(reloaded.repl/set-init! new-system)

