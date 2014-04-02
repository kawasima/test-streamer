package test_streamer.client;

/**
 * @author kawasima
 */
public interface ClientUI {
    public void running(String testName);
    public void disconnect();
    public void standby();
}
