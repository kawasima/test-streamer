(defproject net.unit8/test-streamer-server "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [hiccup "1.0.5"]
                 [net.unit8.wscl/websocket-classloader "0.1.0-SNAPSHOT"]
                 [junit/junit "4.11"]
                 [org.clojure/tools.logging "0.2.6"]
                 [ring/ring-core "1.2.1" :exclusions [org.clojure/tools.reader]]
                 [ring/ring-devel "1.2.1"]
                 [compojure "1.1.6"]
                 [aleph "0.3.2"]
                 [javax.servlet/servlet-api "2.5"]
                 [org.clojure/clojurescript "0.0-2173"]]

  :source-paths ["src/clj"]
  :plugins [[lein-cljsbuild "1.0.2"]]

  :cljsbuild {
    :builds {
      :main {
        :source-paths ["src/cljs"]
        :jar true
        :compiler {
          :output-to "resources/public/js/test-streamer.js"
          :optimizations :advanced}}}})
