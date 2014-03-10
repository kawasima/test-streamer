(ns test-streamer.core
  (:use [goog.dom :only [getElement]]
        [goog.event :only [listen]]))

(defn []
  (let [submit-form (getElement "submit-form")]
    (listen submit-form EventType/SUBMIT))
  )
