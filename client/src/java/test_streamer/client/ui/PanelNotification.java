package test_streamer.client.ui;

import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import test_streamer.client.ClientUI;
import test_streamer.client.dto.TestSuiteResult;

/**
 * @author kawasima
 */
public class PanelNotification extends AnchorPane implements ClientUI {
    private Label statusBar;
    private TextArea messageArea;
    private VBox vBox = new VBox();

    public PanelNotification() {
        statusBar = new Label("Connecting...");
        messageArea = new TextArea();
        messageArea.setDisable(true);
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
        statusBar.setText("Disconnected");
        statusBar.setStyle("-fx-text-fill: #ff0000;");
    }

    public void standby() {
        statusBar.setText("Connected");
        statusBar.setStyle("-fx-text-fill: #00ff00;");
    }
}
