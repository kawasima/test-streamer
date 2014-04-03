package test_streamer.client.ui;

import test_streamer.client.ClientUI;
import test_streamer.client.dto.TestSuiteResult;

import javax.swing.*;
import java.awt.*;

/**
 * @author kawasima
 */
public class PanelNotification extends JPanel implements ClientUI {
    private StatusBar statusBar;
    private MessageArea messageArea;
    public PanelNotification() {
        setLayout(new BorderLayout());
        statusBar = new StatusBar("Connecting...");
        messageArea = new MessageArea();
        add(statusBar, BorderLayout.NORTH);
        add(new JScrollPane(messageArea), BorderLayout.CENTER);
    }

    @Override
    public void beginTest(String testName) {
        messageArea.append("Running tests: " + testName + "\n");
    }

    @Override
    public void endTest(TestSuiteResult result) {
        messageArea.append(result.toString() + "\n");
    }

    @Override
    public void disconnect() {
        messageArea.append("Disconnect from server." + "\n");
        statusBar.setText("Disconnected", Color.RED);
    }

    @Override
    public void standby() {
        statusBar.setText("Connected", Color.GREEN);
    }
}
