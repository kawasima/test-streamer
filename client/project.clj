(defproject net.unit8/test-streamer-client "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [net.unit8.wscl/websocket-classloader "0.2.1"]
                 [io.undertow/undertow-websockets-jsr "1.1.1.Final"]
                 [org.slf4j/slf4j-simple "1.7.21"]
                 [us.bpsm/edn-java "0.4.6"]
                 [junit "4.12"]]
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :aot [test-streamer.client.core test-streamer.client.applet]
  :main test-streamer.client.core
  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]
  :uberjar-name "client.jar"
  :manifest {"Manifest-Version" "1.0"
             "Permissions" "all-permissions"}
  :pom-plugins [[org.apache.maven.plugins/maven-compiler-plugin "3.3"
                 {:configuration ([:source "1.8"] [:target "1.8"])}]]
  :profiles {:dev {:jvm-opts ["-Dwscl.cache.directory=${user.home}/.wscl-cache"]}})
