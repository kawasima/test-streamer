package test_streamer.client.ui;

import test_streamer.client.ClientUI;

import javax.swing.*;

/**
 * @author kawasima
 */
public class PanelNotification extends JTextArea implements ClientUI {
    public PanelNotification() {
        setColumns(100);
        setRows(25);
    }
    @Override
    public void running(String testName) {
        append("Run " + testName);
    }

    @Override
    public void disconnect() {
        append("Disconnect from server.");
    }

    @Override
    public void standby() {
    }
}
