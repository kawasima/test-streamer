package test_streamer.client;

import test_streamer.client.dto.TestSuiteResult;

/**
 * @author kawasima
 */
public interface ClientUI {
    public void beginTest(String testName);
    public void endTest(TestSuiteResult result);
    public void disconnect();
    public void standby();
}
