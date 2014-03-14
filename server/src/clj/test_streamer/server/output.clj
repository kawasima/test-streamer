(ns test-streamer.server.output
  (:use [hiccup core page])
  (:require [clojure.java.io :as io])
  (:import [java.net InetAddress]))

(defn to-xml [result]
  (html
    (xml-declaration "UTF-8")
    [:testsuite {:name (:result)}]))

(defn jnlp [req]
  (let [localhost (InetAddress/getLocalHost)]
    {:status 200
     :headers {"Content-Type" "application/x-java-jnlp-file"}
     :body (html
            (xml-declaration "UTF-8")
            [:jnlp {:spec "6.0+"
                    :codebase (str "http://"
                                   (.getHostName localhost) ":"
                                   (:server-port req))
                    :href "test-streamer-client.jnlp"}
             [:information
              [:title "TestStreamer client"]
              [:vendor "kawasima"]
              [:offline-allowed]]
             [:security [:all-permissions]]
             [:resources
              [:property {:name "jnlp.packEnabled" :value "false"}]
              [:jar {:href (str "http://"
                                   (.getHostName localhost) ":"
                                   (:server-port req) "/client.jar")}]]
             [:application-desc {:main-class "test_streamer.client.core"}
              [:argument (str "ws://" (.getHostName localhost) ":"
                              (:server-port req))]]])}))

(defn client-jar [& {compress? :compress}]
  {:status 200
   :headers {"Content-Type" (if compress?
                              "application/x-java-pack200"
                              "application/java-archiver")}
   :body (io/input-stream (if compress? "client.jar.pack.gz" "client.jar"))})
