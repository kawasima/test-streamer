(ns test-streamer.server.test-shots
  (:require [test-streamer.server.page :as page]
            [test-streamer.server.output :as output])
  (:use [liberator.core :only [defresource]]
        [lamina.core]
        [clojure.tools.logging :only [info debug]])
  (:import [net.unit8.wscl ClassProvider]
           [java.nio.file FileSystems Paths Files SimpleFileVisitor FileVisitResult LinkOption]))

(defonce entries (atom {}))
(defonce class-provider (ClassProvider.))

(def shots-queue (permanent-channel))

(defn progress [shot-id]
  (let [shot (get @entries shot-id)
        prog (- 100 (float (/ (* 100 (count (filter nil? (vals (:results shot)))))
                             (count (vals (:results shot))))))]
    (swap! entries assoc-in [shot-id :progress] prog)
    prog))

(defn- vectorize [v]
  (cond
    (coll? v) (vec v)
    (empty? v) nil
    :default [v]))

(defn- file-to-class [path]
  (.replace
    (.replaceAll (str path) "\\.class$" "")
    (.getSeparator (FileSystems/getDefault)) "."))

(defn- scan-tests-internal [roots ptn]
  (let [matcher (.getPathMatcher (FileSystems/getDefault)
                  (str "glob:" (clojure.string/replace ptn #"\.java$" ".class")))
        tests (atom [])]
    (doseq [root roots]
      (Files/walkFileTree root
        (proxy [SimpleFileVisitor] []
          (visitFile [path attrs]
            (let [rel-path (.relativize root path)]
              (when (.matches matcher rel-path)
                (swap! tests conj (file-to-class rel-path))))
            FileVisitResult/CONTINUE))))
    (not-empty @tests)))

(defn- scan-tests [includes classpaths]
  (let [roots (->> classpaths
                   (filter #(= (.getProtocol %) "file"))
                   (map #(Paths/get (.toURI %)))
                   (filter #(Files/isDirectory % (make-array LinkOption 0))))]
    (some->> includes
             (keep (partial scan-tests-internal roots))
             not-empty
             flatten)))

(defn submit-tests [shot-id tests & {classpaths :classpaths}]
  (let [classloader-id (and classpaths (.registerClasspath class-provider
                                         (into-array java.net.URL classpaths)))]
    (swap! entries assoc (.toString shot-id)
      {:id shot-id
       :submitted-at (java.util.Date.)
       :progress 0.0
       :complete? (promise)
       :classloader-id  classloader-id
       :results (apply hash-map (interleave tests (repeat nil)))})
    (doseq [test tests]
      (enqueue shots-queue (pr-str {:shot-id shot-id
                                    :name test
                                    :classloader-id classloader-id})))
    shot-id))

(defresource list-test-shots
  :allowed-methods [:get :post]
  :available-media-types ["application/json" "text/html"]
  :can-post-to-missing? (fn [{{{:strs [include cp]} :form-params} :request}]
                          (let [classpaths (if (vectorize cp)
                                             (map #(java.net.URL. %) cp)
                                             (.. (Thread/currentThread) getContextClassLoader getURLs))]
                            (when-let [tests (scan-tests (vectorize include) classpaths)]
                              {::tests tests ::classpaths classpaths})))
  :exists? false
  :post! (fn [{classpaths ::classpaths tests ::tests {{shot-id :shotId} :params} :request}]
           (let [shot-id (if shot-id
                           (java.util.UUID/fromString shot-id)
                           (java.util.UUID/randomUUID))]
             (submit-tests shot-id tests
               :classpaths classpaths)
             {::id shot-id}))
           
  :post-redirect? (fn [ctx]
                    (case (get-in ctx [:representation :media-type])
                      ("text/html" "application/xhtml+xml")
                      {:location (str "/test-shots/" (get ctx ::id))}
                      false)))

(defresource entry-test-shot [id]
  :allowed-methods [:get :delete]
  :delete! (fn [ctx]
            (swap! entries dissoc id))
  :available-media-types ["application/json" "text/html"]
  :handle-ok (fn [ctx]
               (case (get-in ctx [:representation :media-type])
                 ("text/html" "application/xhtml+xml")
                 (page/report-page (get @entries id))
                 ("application/json")
                 (get @entries id))))

(defresource entry-test-shot-report [id]
  :allowd-methods [:get]
  :available-media-types ["text/xml" "text/html"]
  :service-available? (fn [ctx]
                        (if-let  [complete? (get-in @entries [id :complete?])]
                          (when (deref complete? 30000 false)
                            {::xml (output/to-xml (get-in @entries [id :results]))})
                          true))
  :exists? ::xml
  :handle-ok ::xml)
