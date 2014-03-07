(ns test-streamer.page
  (:use [hiccup core page]))

(defn index-page [shots]
  (html5
    [:head]
    [:body
      [:table
        (for [shot shots]
          [:tr
            [:td (:shot-id shot)]
            [:td (:submitted-at shot)]])]]))
