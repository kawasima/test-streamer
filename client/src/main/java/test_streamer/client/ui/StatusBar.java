package test_streamer.client.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import test_streamer.client.ClientStatus;
import test_streamer.client.event.ConnectEvent;

/**
 * @author kawasima
 */
public class StatusBar extends HBox {
    private Label statusText;
    private Button reconnectBtn;
    private ClientStatus status;

    public StatusBar(ClientStatus status) {
        statusText = new Label("Connecting...");
        HBox.setHgrow(statusText, Priority.ALWAYS);
        statusText.setMaxWidth(Double.MAX_VALUE);
        reconnectBtn = new Button("Reconnect");
        reconnectBtn.getStyleClass().add("btn-reconnect");
        reconnectBtn.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            getParent().fireEvent(new ConnectEvent());
        });
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("status-bar");
        setHeight(50.0);
        getChildren().addAll(statusText, reconnectBtn);
        setStatus(status);
    }

    public void setStatus(ClientStatus status) {
        this.status = status;
        statusText.setText(status.getLabel());
        statusText.setStyle(status.getStyle());
        reconnectBtn.setVisible(status == ClientStatus.DISCONNECTED);
    }

    public ClientStatus getStatus() {
        return status;
    }
}
