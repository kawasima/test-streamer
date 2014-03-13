(ns test-streamer.server.page
  (:use [hiccup core page element]))

(defmacro layout [& body]
  `(html5
     [:head
       [:link {:rel "stylesheet" :href "/css/pure-min.css"}]
       [:link {:rel "stylesheet" :href "/css/test-streamer.css"}]
       [:script {:src "/js/test-streamer.js"}]]
     [:body
       [:div.header
         [:div.pure-menu.pure-menu-horizontal
           [:a.pure-menu-heading {:href "/"} "Test Streamer"]]]
       [:div#content.pure-g
         [:div.pure-u-1 ~@body]]]))

(defn index-page [& {shots :shots clients :clients}]
  (layout
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
        (for [[shot-id shot] (sort-by #(-> (second %) :updated-at) shots)]
          (let [progress (- 100 (float (/ (* 100 (count (filter nil? (vals (:results shot)))))
                                         (count (vals (:results shot))))))]
            [:tr
              [:td 
                [:a {:href (str "/report/" shot-id)} shot-id]]
              [:td (:submitted-at shot)]
              [:td.number (format "%.1f%%" progress)]
              [:td
                (when (= progress 100.0)
                  [:a {:href (str "/report" shot-id ".xml")} "report"])]]))])
    
    [:h2.content-subhead "Submit tests"]
    [:form.pure-form {:method "post" :action "/submit"}
      [:fieldset
        [:legend "Test classes"]
        [:input {:type "text" :name "test"}]
        [:button.pure-button.pure-button-primary {:type "submit"} "execute"]]]))

(defn report-page [shot]
  (layout
    [:h2 "All Tests"]
    [:table.pure-table
      [:thead
        [:tr
          [:th "Name"]
          [:th "Tests"]
          [:th "Failures"]
          [:th "Errors"]
          [:th "Duration"]]]
      (for [[test-class result] (sort (:results shot))]
        [:tr
          [:td test-class]
          (if result
            (html
              [:td.number (:tests result)]
              [:td.number (:failures result)]
              [:td.number (:errors result)]
              [:td.number (format "%.3f" (float (/ (:time result) 1000)))])
            [:td {:colspan 4} "Wait for executing..."])])]

    (for [[test-class result] (sort (:results shot))]
      (when (and result (> (:errors result) 0))
        (html
          [:h2 "All Failed Tests"]
          [:table.pure-table.pure-table-bordered
            [:thead
              [:tr
                [:th "Test Name"]
                [:th "Duration"]]]
            (for [tc (:testcases result) :when (or (:error tc) (:failure tc))]
              [:tr.failed-test
                [:td
                  [:a.failed-test-name {:href "#"} (str (:className tc) "." (:methodName tc))]
                  [:div.failed-test-detail
                    [:pre (or (get-in tc [:error :stacktrace])
                            (get-in tc [:failure :stacktrace]))]]]
                [:td.number (format "%.3f" (float (/ (:time tc) 1000)))]])])))
    (javascript-tag
      "test_streamer.core.setup_report()")))

(defn classpaths-page [classpaths]
  (layout
    [:form.pure-form {:method "POST"}
      [:fieldset
        [:input.pure-input-1-2 {:type "text" :name "path"}]
        [:button.pure-button.pure-button-primary {:type "submit"} "Add"]]]
    [:ul
      (for [path classpaths]
        [:li path])]))

(defn client-page []
  (layout
    [:a {:href "http://labs.unit8.net/test-streamer-client.jnlp"} "Launch Client"]))
