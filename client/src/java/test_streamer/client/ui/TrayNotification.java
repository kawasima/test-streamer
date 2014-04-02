package test_streamer.client.ui;

import test_streamer.client.ClientUI;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * @author kawasima
 */
public class TrayNotification implements ClientUI {
    private static final BufferedImage IMG_RUNNING;
    private static final BufferedImage IMG_DISCONNECT;
    private static final BufferedImage IMG_STANDBY;
    static {
        try {
            IMG_RUNNING    = ImageIO.read(TrayNotification.class.getResource("/running.gif"));
            IMG_DISCONNECT = ImageIO.read(TrayNotification.class.getResource("/disconnect.gif"));
            IMG_STANDBY    = ImageIO.read(TrayNotification.class.getResource("/standby.gif"));
        } catch(IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private TrayIcon icon;

    public TrayNotification() {
        SystemTray tray = SystemTray.getSystemTray();
        PopupMenu menu = new PopupMenu();
        Menu exitMenu = new Menu("Exit");
        icon = new TrayIcon(IMG_DISCONNECT, "TestStreamer", menu);

        exitMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.exit(0);
            }
        });

        menu.add(exitMenu);
        try {
            tray.add(icon);
        } catch(AWTException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void running(String testName) {
        icon.setImage(IMG_RUNNING);
        icon.displayMessage("TestStreamer", "Executing test " + testName, TrayIcon.MessageType.INFO);
    }

    @Override
    public void standby() {
        icon.setImage(IMG_STANDBY);
    }

    @Override
    public void disconnect() {
        icon.setImage(IMG_DISCONNECT);
        icon.displayMessage("TestStreamer", "Disconnected from server.", TrayIcon.MessageType.WARNING);
    }

    public void removeTrayIcon() {
        SystemTray.getSystemTray().remove(icon);
    }


}
