(ns test-streamer.client.ui
  (:require [clojure.java.io :as io])
  (:import [java.awt SystemTray TrayIcon TrayIcon$MessageType
             MenuItem]
           [javax.imageio ImageIO]))

(def ^:dynamic *icon*)

(def img-running    (ImageIO/read (io/resource "running.gif")))
(def img-disconnect (ImageIO/read (io/resource "disconnect.gif")))
(def img-standby    (ImageIO/read (io/resource "standby.gif")))

(defn create-tray-icon []
  (let [tray (SystemTray/getSystemTray)
        icon (TrayIcon. img-disconnect "TestStreamer")]
    (.add tray icon)
    (alter-var-root (var *icon*)
      (constantly icon))
    icon))

(defn running [test]
  (.setImage *icon* img-running)
  (.displayMessage *icon* "TestStreamer" (str "Executing test " test)
    TrayIcon$MessageType/WARNING))

(defn standby []
  (.setImage *icon* img-standby))

(defn disconnect []
  (.setImage *icon* img-disconnect)
  (.displayMessage *icon* "TestStreamer" "Disconnected from server."))

(defn remove-tray-icon []
  (let [tray (SystemTray/getSystemTray)]
    (.remove tray *icon*)))


