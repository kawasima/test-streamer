(ns test-streamer.server.component.webapp
  (:require [com.stuartsierra.component :as component]
            [compojure.core :refer [GET ANY routes]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            (test-streamer.server [page :as page]
                                  [output :as output])))

(defrecord WebappComponent [test-shots dispatcher]
  component/Lifecycle

  (start [component]
    (assoc component :handler
           (-> (routes
                (ANY "/test-shots" [:as req]
                  (get-in test-shots [:resources :list-test-shots]))
                (ANY "/test-shots/:shot-id" [shot-id]
                  (let [entry-test-shot (get-in test-shots [:resources :entry-test-shot])]
                    (entry-test-shot shot-id)))
                (GET "/test-shots/:shot-id/report" [shot-id]
                  (let [entry-test-shot-report (get-in test-shots [:resources :entry-test-shot-report])]
                    (entry-test-shot-report shot-id)))
                (GET "/client" []
                  (page/client-page))
                (GET "/test-streamer-client.jnlp" [:as request]
                  (output/jnlp request))
                (GET "/client.jar" [:as request]
                  (output/client-jar))
                (GET "/client.jar.pack.gz" [:as request]
                  (output/client-jar :compress true))
                (GET "/" []
                  (page/index-page :shots   @(:entries test-shots)
                                   :clients @(:clients dispatcher)))
                (route/resources "/")
                (route/not-found "Not Found"))
               (wrap-defaults (-> site-defaults
                                  (assoc-in [:security :anti-forgery] false))))))

  (stop [component]
    (dissoc component :handler)))

(defn webapp-component [options]
  (map->WebappComponent options))

