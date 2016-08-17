package test_streamer.client.ui;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import test_streamer.client.ClientUI;
import test_streamer.client.dto.TestSuiteResult;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author kawasima
 */
public class TrayNotification implements ClientUI {
    private static final BufferedImage IMG_RUNNING;
    private static final BufferedImage IMG_DISCONNECT;
    private static final BufferedImage IMG_STANDBY;

    private ConcurrentHashMap<EventType, EventHandler> handlers = new ConcurrentHashMap<>();

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
        MenuItem exitMenu = new MenuItem("Exit");
        icon = new TrayIcon(IMG_DISCONNECT, "TestStreamer", menu);

        exitMenu.addActionListener(actionEvent -> {
            System.exit(0);
        });

        menu.add(exitMenu);
        try {
            tray.add(icon);
        } catch(AWTException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void beginTest(String testName) {
        icon.setImage(IMG_RUNNING);
        icon.displayMessage("TestStreamer", "Executing test " + testName, TrayIcon.MessageType.INFO);
    }

    @Override
    public void endTest(TestSuiteResult result) {
    }

    @Override
    public void standby() {
        icon.setImage(IMG_STANDBY);
    }

    @Override
    public <T extends javafx.event.Event> void addEventHandler(EventType<T> eventType, EventHandler<? super T> handler) {
        handlers.putIfAbsent(eventType, handler);
    }

    @Override
    public void fireEvent(Event event) {
        Optional.ofNullable(handlers.get(event.getEventType()))
                .ifPresent(handler -> handler.handle(event));
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
