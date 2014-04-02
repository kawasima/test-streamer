(ns test-streamer.server.output
  (:use [hiccup core page])
  (:require [clojure.java.io :as io])
  (:import [java.net InetAddress]))

(defn to-xml [results]
  (html {:mode :xml}
    (xml-declaration "UTF-8")
    [:testsuites
      (for [[id result] results]
        [:testsuite (select-keys result [:name :time :tests :failures :errors :skipped])
          (for [tc (:testcases result)]
            [:testcase (select-keys tc [:time :classname :name])
              (cond
                (:failure tc)
                [:failure (select-keys (:failure tc) [:type :message]) (h (get-in tc [:failure :stacktrace]))]

                (:error tc)
                [:error (select-keys (:error tc) [:type :message]) (h (get-in tc [:error :stacktrace]))]

                (:skip? tc)
                [:skipped])])])]))

(defn jnlp [req]
  (let [localhost (InetAddress/getLocalHost)]
    {:status 200
     :headers {"Content-Type" "application/x-java-jnlp-file"}
     :body (html
            (xml-declaration "UTF-8")
            [:jnlp {:spec "6.0+"
                    :codebase (str "http://"
                                   (:server-name req) ":"
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
                                   (:server-name req) ":"
                                   (:server-port req) "/client.jar")}]]
             [:application-desc {:main-class "test_streamer.client.core"}
              [:argument (str "ws://" (:server-name req) ":"
                              (:server-port req))]]])}))

(defn client-jar [& {compress? :compress}]
  {:status 200
   :headers {"Content-Type" (if compress?
                              "application/x-java-pack200"
                              "application/java-archiver")}
   :body (io/input-stream (if compress? "client.jar.pack.gz" "client.jar"))})
