package test_streamer.client.ui;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import test_streamer.client.ClientStatus;
import test_streamer.client.ClientUI;
import test_streamer.client.dto.TestSuiteResult;

/**
 * @author kawasima
 */
public class PanelNotification extends AnchorPane implements ClientUI {
    private ImageView logo;
    private StatusBar statusBar;
    private TextArea messageArea;
    private VBox vBox = new VBox(5.0);

    public PanelNotification() {
        logo = new ImageView();
        logo.setImage(new Image(getClass().getResourceAsStream("/logo.png")));

        messageArea = new TextArea();
        messageArea.setEditable(false);
        VBox.setVgrow(messageArea, Priority.ALWAYS);
        messageArea.setMaxHeight(Double.MAX_VALUE);
        statusBar = new StatusBar(ClientStatus.CONNECTING);
        vBox.getChildren().addAll(logo, statusBar, messageArea);
        getChildren().addAll(vBox);
        setStyle("-fx-padding: 10");
        AnchorPane.setTopAnchor(vBox, 0.0);
        AnchorPane.setBottomAnchor(vBox, 0.0);
        AnchorPane.setLeftAnchor(vBox, 0.0);
        AnchorPane.setRightAnchor(vBox, 0.0);
    }

    public void beginTest(String testName) {
        Platform.runLater(() ->
                messageArea.appendText("Running tests: " + testName + "\n"));

    }

    public void endTest(TestSuiteResult result) {
        Platform.runLater(() ->
                messageArea.appendText(result.toString() + "\n"));

    }

    public void disconnect() {
        Platform.runLater(() -> {
            messageArea.appendText("Disconnect from server." + "\n");
            statusBar.setStatus(ClientStatus.DISCONNECTED);
        });
    }

    public void standby() {
        Platform.runLater(() -> {
            statusBar.setStatus(ClientStatus.CONNECTED);
        });
    }
}
