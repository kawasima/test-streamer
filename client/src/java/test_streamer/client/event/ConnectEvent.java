package test_streamer.client.event;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * @author kawasima
 */
public class ConnectEvent extends Event {
    public static final EventType<ConnectEvent> CONNECT_SERVER = new EventType(ANY, "CONNECT_SERVER");

    public ConnectEvent() {
        this(CONNECT_SERVER);
    }

    public ConnectEvent(EventType<? extends Event> type) {
        super(type);
    }
}
