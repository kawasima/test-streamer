package test_streamer.client;

/**
 * @author kawasima
 */
public enum ClientStatus {
    CONNECTING("Connecting...", "-fx-text-fill: #009900;"),
    CONNECTED("Connected", "-fx-text-fill: #009900;"),
    DISCONNECTED("Disconnected", "-fx-text-fill: #990000;");

    private String label;
    private String style;

    ClientStatus(String label, String style) {
        this.label = label;
        this.style = style;
    }

    public String getLabel() {
        return label;
    }

    public String getStyle() {
        return style;
    }
}
