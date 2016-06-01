package test_streamer.client.ui;

import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import test_streamer.client.ClientStatus;
import test_streamer.client.ClientUI;
import test_streamer.client.dto.TestSuiteResult;

/**
 * @author kawasima
 */
public class PanelNotification extends AnchorPane implements ClientUI {
    private StatusBar statusBar;
    private TextArea messageArea;
    private VBox vBox = new VBox();

    public PanelNotification() {
        messageArea = new TextArea();
        messageArea.setEditable(false);
        statusBar = new StatusBar(ClientStatus.CONNECTING);
        vBox.getChildren().addAll(statusBar, messageArea);
        getChildren().addAll(vBox);

    }

    public void beginTest(String testName) {
        messageArea.appendText("Running tests: " + testName + "\n");
    }

    public void endTest(TestSuiteResult result) {
        messageArea.appendText(result.toString() + "\n");
    }

    public void disconnect() {
        messageArea.appendText("Disconnect from server." + "\n");
        statusBar.setStatus(ClientStatus.DISCONNECTED);
    }

    public void standby() {
        statusBar.setStatus(ClientStatus.CONNECTED);
    }
}
