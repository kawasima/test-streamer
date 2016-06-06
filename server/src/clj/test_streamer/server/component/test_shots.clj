(ns test-streamer.server.component.test-shots
  (:require [com.stuartsierra.component :as component]
            [test-streamer.server.page :as page]
            [test-streamer.server.output :as output]
            [liberator.core :as liberator]
            [hiccup.util :refer [url]]
            [clojure.tools.logging :refer [info debug]]
            [clojure.core.async :refer [go chan put!]])
  (:import [net.unit8.wscl ClassLoaderHolder]
           [java.nio.file FileSystems Paths Files SimpleFileVisitor FileVisitResult LinkOption]))

(defn- vectorize
  "Convert a given value to a vector."
  [v]
  (cond
    (coll? v) (vec v)
    (empty? v) nil
    :default [v]))

(defn- file-to-class
  "Convert a file path to the canonical name of a class."
  [path]
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

(defn scan-tests [includes classpaths]
  (let [roots (->> classpaths
                   (filter #(= (.getProtocol %) "file"))
                   (map #(Paths/get (.toURI %)))
                   (filter #(Files/isDirectory % (make-array LinkOption 0))))]
    (some->> includes
             (keep (partial scan-tests-internal roots))
             not-empty
             flatten)))

(defn submit-tests [shot-id tests entries shots-queue & {classpaths :classpaths}]
  (let [classloader-id (and classpaths (.registerClasspath
                                        (ClassLoaderHolder/getInstance)
                                        (into-array java.net.URL classpaths)
                                        (.. (Thread/currentThread) getContextClassLoader)))]
    (swap! entries assoc (.toString shot-id)
      {:id shot-id
       :submitted-at (java.util.Date.)
       :progress 0.0
       :complete? (promise)
       :classloader-id  classloader-id
       :results (apply hash-map (interleave tests (repeat nil)))})
    (doseq [test tests]
      (put! shots-queue {:shot-id shot-id
                         :name test
                         :classloader-id classloader-id}))
    shot-id))

(defn classpath [loader]
  (distinct
   (mapcat #(seq (.getURLs %))
           (take-while identity
                       (iterate
                        #(.getParent ^ClassLoader %) loader)))))

;; The resource of test shots.
(defn build-resources [shots-queue entries]
  {:list-test-shots
   (liberator/resource
    :allowed-methods [:get :post]
    :available-media-types ["application/json" "text/html"]
    :can-post-to-missing? (fn [{{{:strs [include cp]} :form-params} :request :as ctx}]
                            (let [classpaths (if (vectorize cp)
                                               (map #(java.net.URL. %) cp)
                                               (classpath (clojure.lang.RT/baseLoader)))]
                              (when-let [tests (scan-tests (vectorize include) classpaths)]
                                {::tests tests ::classpaths classpaths})))
    :exists? false
    :post! (fn [{classpaths ::classpaths tests ::tests {{shot-id :shotId} :params} :request}]
             (let [shot-id (if shot-id
                             (java.util.UUID/fromString shot-id)
                             (java.util.UUID/randomUUID))]
               (submit-tests shot-id tests entries shots-queue
                             :classpaths classpaths)
               {::id shot-id}))

    :post-redirect? (fn [ctx]
                      (case (get-in ctx [:representation :media-type])
                        ("text/html" "application/xhtml+xml")
                        {:location (str (url "test-shots/" (get ctx ::id)))}
                        false))
    :handle-ok "ok")

   :entry-test-shot
   (fn [id]
     (liberator/resource
      :allowed-methods [:get :delete]
      :delete! (fn [ctx]
                 (swap! entries dissoc id))
      :available-media-types ["application/json" "text/html"]
      :handle-ok (fn [ctx]
                   (case (get-in ctx [:representation :media-type])
                     ("text/html" "application/xhtml+xml")
                     (page/report-page (get @entries id))
                     ("application/json")
                     (get @entries id)))))
   
   :entry-test-shot-report
   (fn [id]
     (liberator/resource
      :allowd-methods [:get]
      :available-media-types ["text/xml" "text/html"]
      :service-available? (fn [ctx]
                            (if-let  [complete? (get-in @entries [id :complete?])]
                              (when (deref complete? 30000 false)
                                {::xml (output/to-xml (get-in @entries [id :results]))})
                              true))
      :exists? ::xml
      :handle-ok ::xml))})

(defrecord TestShots []
  component/Lifecycle

  (start [component]
    (let [shots-queue (chan)
          entries     (atom {})]
      (assoc component
           :resources (build-resources shots-queue entries)
           :shots-queue shots-queue
           :entries     entries)))

  (stop [component]
    (dissoc component :shots-queue :entries)))

(defn test-shots-component [options]
  (map->TestShots options))
