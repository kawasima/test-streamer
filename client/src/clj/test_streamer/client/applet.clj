(ns test-streamer.client.applet
  (:import
    (javax.swing JApplet JPanel JLabel JFrame))
  (:gen-class
    :post-init post-init
    :extends javax.swing.JApplet))
 
(defn -post-init [this]
  (def jpanel (JPanel.))
  (.add jpanel (JLabel. "This is my first applet"))
  (.setContentPane this jpanel))

