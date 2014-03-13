(ns test-streamer.client.ui
  (:require [clojure.java.io :as io])
  (:import [java.awt SystemTray TrayIcon TrayIcon$MessageType
             MenuItem PopupMenu MenuItem]
           [java.awt.event ActionListener]
           [javax.imageio ImageIO]))

(def ^:dynamic *icon*)

(def img-running    (ImageIO/read (io/resource "running.gif")))
(def img-disconnect (ImageIO/read (io/resource "disconnect.gif")))
(def img-standby    (ImageIO/read (io/resource "standby.gif")))

(defn create-tray-icon []
  (let [tray (SystemTray/getSystemTray)
        menu (PopupMenu.)
        exit-menu (MenuItem. "Exit")
        icon (TrayIcon. img-disconnect "TestStreamer" menu)]
    (.addActionListener
     exit-menu
     (proxy [ActionListener] []
       (actionPerformed [event]
         (System/exit 0))))
    (.add menu exit-menu)
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
  (.displayMessage *icon* "TestStreamer" "Disconnected from server."
    (TrayIcon$MessageType/WARNING)))

(defn remove-tray-icon []
  (let [tray (SystemTray/getSystemTray)]
    (.remove tray *icon*)))


