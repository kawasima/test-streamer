(ns user
  (:refer-clojure :exclude [test])
  (:require  [clojure.repl :refer :all]
             [clojure.pprint :refer [pprint]]
             [clojure.tools.namespace.repl :refer [refresh]]
             [reloaded.repl :refer [system init start stop go reset]]
             [meta-merge.core :refer [meta-merge]]
             [duct.component.figwheel :as figwheel]
             (test-streamer.server [system :as system]
                                   [config :as config])))

(def dev-config
  {:figwheel
   {:css-dirs ["resources/public/css"]
    :builds   [{:source-paths ["src" "dev"]
                :build-options
                {:optimizations :none
                 :main "cljs.user"
                 :asset-path "/js"
                 :output-to  "target/figwheel/test-streamer/public/js/main.js"
                 :output-dir "target/figwheel/test-streamer/public/js"
                 :source-map true
                 :source-map-path "/js"}}]}})

(def config
  (meta-merge config/defaults
              config/environ
              dev-config))

(defn new-system []
  (into (system/new-system {})
        {:figwheel (figwheel/server (:figwheel config))}))

(reloaded.repl/set-init! new-system)

