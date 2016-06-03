package test_streamer.client;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import test_streamer.client.dto.TestSuiteResult;

/**
 * @author kawasima
 */
public interface ClientUI {
    void beginTest(String testName);
    void endTest(TestSuiteResult result);
    void disconnect();
    void standby();
    <T extends Event> void addEventHandler(EventType<T> var1, EventHandler<? super T> var2);
    void fireEvent(Event event);
}
