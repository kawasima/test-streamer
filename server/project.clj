(defproject net.unit8/test-streamer-server "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [hiccup "1.0.5"]
                 [net.unit8.wscl/websocket-classloader "0.2.1"]
                 [junit/junit "4.12"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-simple "1.7.21"]
                 
                 [io.undertow/undertow-websockets-jsr "1.1.1.Final"]
                 [ring "1.4.0" :exclusions [org.clojure/tools.reader]]
                 [ring/ring-defaults "0.2.0"]

                 [compojure "1.5.0"]
                 [liberator "0.14.1"]
                 [javax.servlet/servlet-api "2.5"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/clojurescript "1.8.51"]]

  :source-paths ["src/clj"]
  :plugins [[lein-cljsbuild "1.1.3"]]
  :aliases {"run-task" ["with-profile" "+repl" "run" "-m"]}

  :profiles
  {:dev  [:project/dev  :profiles/dev]
   :test [:project/test :profiles/test]
   :uberjar {:aot :all}
   :profiles/dev  {}
   :profiles/test {}
   :project/dev  {:dependencies [[reloaded.repl "0.2.1"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/tools.nrepl "0.2.12"]]
                   :source-paths ["dev"]
                   :repl-options {:init-ns user}}
   :project/test {}}
  :cljsbuild {
    :builds {
      :main {
        :source-paths ["src/cljs"]
        :jar true
        :compiler {
          :output-to "resources/public/js/test-streamer.js"
          :optimizations :advanced}}}})
