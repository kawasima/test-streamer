(ns test-streamer.server.page
  (:use [hiccup core page]))

(defmacro layout [& body]
  `(html5
     [:head
       [:link {:rel "stylesheet" :href "//yui.yahooapis.com/pure/0.4.2/pure-min.css"}]
       [:link {:rel "stylesheet" :href "/css/test-streamer.css"}]]
     [:body
       [:div.header
         [:div.pure-menu.pure-menu-open.pure-menu-horizontal
           [:a.pure-menu-heading {:href "#"} "Test Streamer"]]]
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
    [:table.pure-table
      (for [[shot-id shot] (sort-by #(-> (second %) :updated-at) shots)]
        [:tr
          [:td 
            [:a {:href (str "/report/" shot-id)} shot-id]]
          [:td (:submitted-at shot)]])]
    [:h2.content-subhead "Submit tests"]
    [:form.pure-form {:method "post" :action "/submit"}
      [:fieldset
        [:legend "Test classes"]
        [:input {:type "text" :name "test"}]
        [:button.pure-button.pure-button-primary {:type "submit"} "execute"]]]))

(defn report-page [shot]
  (layout
    [:table.pure-table
      [:thead
        [:tr
          [:th "Name"]
          [:th "Tests"]
          [:th "Failures"]
          [:th "Errors"]
          [:th "Times"]]]
      (for [[test-class result] (sort (:results shot))]
        [:tr
          [:td test-class]
          [:td (:tests result)]
          [:td (:failures result)]
          [:td (:errors result)]
          [:td (:time result)]])]

    (for [[test-class result] (sort (:results shot))]
      (when (and result (> (:errors result) 0))
        (for [tc (:testcases result) :when (or (:error tc) (:failure tc))]
          [:table.pure-table
            [:tr
              [:td (:methodName tc)]
              [:td
                [:pre (or (:error tc ) (:failure tc)) ]]]])))))

(defn client-page []
  (layout
    [:a {:href "http://labs.unit8.net/test-streamer-client.jnlp"} "Launch Client"]))
