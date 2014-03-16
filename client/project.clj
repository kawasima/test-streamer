(defproject net.unit8/test-streamer-client "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [net.unit8.wscl/websocket-classloader "0.1.1"]
                 [junit/junit "4.11"]
                 [aleph "0.3.2"]]
  :source-paths ["src/clj"]
  :aot [test-streamer.client.core]
  :main test-streamer.client.core
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :uberjar-name "client.jar"
  :manifest {"Manifest-Version" "1.0"
             "Permissions" "all-permissions"})
