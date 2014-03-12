(ns test-streamer.core
  (:require [goog.dom :as dom]
            [goog.ui.AnimatedZippy :as AnimatedZippy]))

(defn ^:export setup-report []
  (doseq [failed-test (array-seq (dom/getElementsByClass "failed-test"))]
    (goog.ui.AnimatedZippy.
      (dom/getElementByClass "failed-test-name" failed-test)
      (dom/getElementByClass "failed-test-detail" failed-test)
      false)))
