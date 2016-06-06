(ns test-streamer.server.page
  (:use [hiccup core page element])
  (:require [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defmacro layout [{headers :headers} & body]
  `(html5
     [:head
       [:link {:rel "stylesheet" :href "/css/pure-min.css"}]
       [:link {:rel "stylesheet" :href "/css/test-streamer.css"}]
       [:script {:src "/js/test-streamer.min.js"}]
       ~headers]

     [:body
       [:div.header
         [:div.pure-menu.pure-menu-open.pure-menu-horizontal
          [:a.pure-menu-heading {:href "/"}
           [:img {:src "/img/logo.png"}]]
           [:ul
             [:li [:a {:href "/client"} "client"]]]]]
       [:div#content.pure-g
         [:div.pure-u-1 ~@body]]]))

(defn index-page [& {shots :shots clients :clients}]
  (layout {}
    [:h2.content-subhead "Clients"]
    (if (empty? clients)
      "No available clients."
      [:table.pure-table
        [:thead
          [:tr
            [:th "Name"]
            [:th "OS"]
            [:th "CPU"]
            [:th "Status"]]]

        (for [[ch client] clients]
          [:tr
            [:td (:client-name client)]
            [:td (:os-name client) (:os-version client)]
            [:td (:cpu-arch client) " " (:cpu-core client) "cores"]
            [:td (:status client)]])])

    [:h2.content-subhead "Test shots"]
    (if (empty? shots)
      "No shots found."
      [:table.pure-table
        [:thead
          [:tr
            [:th "Shot ID"]
            [:th "Submit Time"]
            [:th "Status"]
            [:th "Report"]]]
        (for [[shot-id shot] (sort-by #(-> (second %) :submitted-at (.getTime)) > shots)]
          (let [progress (- 100 (float (/ (* 100 (count (filter nil? (vals (:results shot)))))
                                         (count (vals (:results shot))))))]
            [:tr
              [:td
                [:a {:href (str "/test-shots/" shot-id)} shot-id]]
              [:td (:submitted-at shot)]
              [:td.number (format "%.1f%%" progress)]
              [:td
                (when (= progress 100.0)
                  [:a {:href (str "/test-shots/" shot-id "/report")} "report"])]]))])

    [:h2.content-subhead "Submit tests"]
    [:form.pure-form {:method "post" :action "/test-shots"}
     (anti-forgery-field)
      [:fieldset
        [:input.pure-input-2-3 {:type "text" :name "include"}]
        [:button.pure-button.pure-button-primary {:type "submit"} "execute"]]]))

(defn report-page [shot]
  (layout {}
    [:h2 "All Tests"]
    [:table.pure-table
      [:thead
        [:tr
          [:th "Name"]
          [:th "Tests"]
          [:th "Failures"]
          [:th "Errors"]
          [:th "Skipped"]
          [:th "Duration"]]]
      (for [[test-class result] (sort (:results shot))]
        [:tr
          [:td test-class]
          (if result
            (html
              [:td.number (:tests result)]
              [:td.number (:failures result)]
              [:td.number (:errors result)]
              [:td.number (:skipped result)]
              [:td.number (format "%.3f" (:time result))])
            [:td {:colspan 5} "Wait for executing..."])])]

    (when (some #(or (> (get (last %) :failures 0) 0)
                     (> (get (last %) :errors 0) 0))
                (:results shot))
      (html
       [:h2 "All Failed Tests"]
       [:table.pure-table.pure-table-bordered
        [:thead
         [:tr
          [:th "Test Name"]
          [:th "Duration"]]]
        (for [[test-class result] (sort (:results shot))]
          (for [tc (:testcases result) :when (or (:error tc) (:failure tc))]
            [:tr.failed-test
             [:td
              [:div.failed-test-name (str (:classname tc) "." (:name tc))]
              [:div.failed-test-detail
               [:pre (or (get-in tc [:error :stacktrace])
                         (get-in tc [:failure :stacktrace]))]]]
             [:td.number (format "%.3f" (:time tc))]]))]))
    (javascript-tag
      "test_streamer.core.setup_report()")))

(defn client-page []
  (layout
   [:h2 "TestStreamer Client"]
   [:p [:a {:href "/test-streamer-client.jnlp"} "Download client"]]))
