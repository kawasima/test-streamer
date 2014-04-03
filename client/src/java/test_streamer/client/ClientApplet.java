package test_streamer.client;

import test_streamer.client.ui.PanelNotification;

import javax.swing.*;
import java.awt.*;
import java.security.*;

import static test_streamer.client.ClientConfig.ClientConfigKey.UI;

/**
 * a client applet.
 *
 * @author kawasima
 */
public class ClientApplet extends JApplet {
    Main main;
    @Override
    public void init() {
        System.setSecurityManager(null);
        Policy.setPolicy(new Policy() {
            @Override
            public PermissionCollection getPermissions(CodeSource codesource) {
                Permissions p = new Permissions();
                p.add(new AllPermission());
                return p;
            }
        });

        ClientConfig config = new ClientConfig();
        PanelNotification panelNotification = new PanelNotification();
        config.setObject(UI, panelNotification);
        main = new Main(config);
    }

    @Override
    public void paint(Graphics g) {
        g.drawString("TestStreamer", 10, 10);

        PanelNotification panelNotification = (PanelNotification)
        add(panelNotification);
        panelNotification.setVisible(true);

        Main main = new Main(config);
        main.start("ws://localhost:5000/join");
    }

    @Override
    public void destroy() {
        main.destroy();
    }

}