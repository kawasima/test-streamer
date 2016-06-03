package test_streamer.client;

import test_streamer.client.dto.TestSuiteResult;

/**
 * @author kawasima
 */
public interface ClientUI {
    void beginTest(String testName);
    void endTest(TestSuiteResult result);
    void disconnect();
    void standby();
}
