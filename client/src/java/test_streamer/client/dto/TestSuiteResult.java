package test_streamer.client.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * A Command for :result
 *
 * @author kawasima
 */
public class TestSuiteResult {
    public TestSuiteResult(String name) {
        this.name = name;
        testcases = new ArrayList<TestCaseResult>();
    }
    private String name;
    private List<TestCaseResult> testcases;
    private int tests;
    private int failures;
    private int errors;
    private int skipped;
    private float time;
    private String clientException;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TestCaseResult> getTestcases() {
        return testcases;
    }

    public int getTests() {
        return tests;
    }

    public void incrementTests() {
        this.tests++;
    }

    public int getFailures() {
        return failures;
    }

    public void incrementFailures() {
        this.failures++;
    }

    public int getErrors() {
        return errors;
    }

    public void incrementErrors() {
        this.errors++;
    }

    public int getSkipped() {
        return skipped;
    }

    public void incrementSkipped() {
        this.skipped++;
    }

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }

    public String getClientException() {
        return clientException;
    }

    public void setClientException(String clientException) {
        this.clientException = clientException;
    }

    public String toString() {
        return new StringBuilder(256)
                .append(name)
                .append(" (tests: ")
                .append(tests)
                .append(", failures: ")
                .append(failures)
                .append(", errors: ")
                .append(errors)
                .append(") ")
                .append(time)
                .append(" sec")
                .toString();
    }
}
