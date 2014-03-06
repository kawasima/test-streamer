(ns test-streamer.core
  (:import [org.junit.runner JUnitCore]
           [net.unit8.websocket.classloader WebSocketClassLoader]))

(defn test2 []
  (let [test-class (Class/forName "example.HogeTest")]
    (.run (JUnitCore.) test-class)))

(let [orig-loader (.getContextClassLoader (Thread/currentThread))
      loader (WebSocketClassLoader. orig-loader "http://localhost:5000")]
  (.setContextClassLoader (Thread/currentThread) loader)
  (try
    (test2)
    (finally (.setContextClassLoader (Thread/currentThread) orig-loader))))
