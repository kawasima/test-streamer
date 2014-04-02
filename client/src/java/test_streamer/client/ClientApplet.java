package test_streamer.client;

import test_streamer.client.ui.PanelNotification;

import javax.swing.*;
import java.awt.*;

import static test_streamer.client.ClientConfig.ClientConfigKey.UI;

/**
 * a client applet.
 *
 * @author kawasima
 */
public class ClientApplet extends JApplet {
    @Override
    public void paint(Graphics g) {
        System.out.println("PAINT");
        g.drawString("TestStreamer", 10, 10);
        System.out.println("RUN");
        ClientConfig config = new ClientConfig();
        PanelNotification panelNotification = new PanelNotification();
        config.setObject(UI, panelNotification);

        add(panelNotification);
        panelNotification.setVisible(true);

        Main main = new Main(config);
        main.start("ws://localhost:5000/join");
    }

}
