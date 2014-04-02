(ns test-streamer.client.applet
  (:import
    (java.awt Graphics Color Font RenderingHints)
    (javax.swing JApplet JButton))
  (:gen-class
    :extends javax.swing.JApplet))
 
(defn -paint [#^JApplet applet #^Graphics g]
  (let [width (.getWidth applet)
        height (.getHeight applet)]
    (doto g
      (.setColor (. Color YELLOW))
      (.fillRect 0 0 width height)
      (.setColor (. Color BLACK))
      (.drawRect 0 0 (dec width) (dec height))
      (.setFont (Font. "Serif" (. Font PLAIN) 24))
      (.drawString "Hello World!" 20 40))))

