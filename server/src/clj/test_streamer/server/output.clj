(ns test-streamer.server.output
  (:use [hiccup.core]))

(defn to-xml [result]
  (html
    (xml-declaration "UTF-8")
    [:testsuite {:name (:result)}]))
