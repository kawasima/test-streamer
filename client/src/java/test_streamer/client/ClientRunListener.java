package test_streamer.client;

import junit.framework.AssertionFailedError;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import test_streamer.client.dto.ErrorResult;
import test_streamer.client.dto.FailureResult;
import test_streamer.client.dto.TestSuiteResult;
import test_streamer.client.dto.TestCaseResult;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * @author kawasima
 */
public class ClientRunListener extends RunListener {
    private Map<String, TestCaseResult> runningCases = new HashMap<String, TestCaseResult>();
    private Map<String, Long> runningTimes = new HashMap<String, Long>();
    private Long suiteStartedTime;
    private TestSuiteResult suiteResult;

    public ClientRunListener(String name) {
        suiteResult = new TestSuiteResult(name);
    }

    public TestSuiteResult getResult() {
        return suiteResult;
    }

    private String extractStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        description.getChildren().get(0);
        suiteStartedTime = System.currentTimeMillis();
    }
    /**
     * Called when an atomic test is about to be started.
     *
     * @param description the description of the test that is about to be run
     * (generally a class and method name)
     */
    @Override
    public void testStarted(Description description) throws Exception {
        TestCaseResult testCaseResult = new TestCaseResult();
        testCaseResult.setClassname(description.getClassName());
        testCaseResult.setName(description.getMethodName());
        runningCases.put(description.getDisplayName(), testCaseResult);
        runningTimes.put(description.getDisplayName(), System.currentTimeMillis());
    }

    /**
     * Called when an atomic test has finished, whether the test succeeds or fails.
     *
     * @param description the description of the test that just ran
     */
    public void testFinished(Description description) throws Exception {
        suiteResult.incrementTests();
        TestCaseResult testCaseResult = runningCases.remove(description.getDisplayName());
        Long startTime = runningTimes.remove(description.getDisplayName());

        testCaseResult.setTime((System.currentTimeMillis() - startTime) / 1000f);
        suiteResult.getTestcases().add(testCaseResult);
    }

    /**
     * Called when an atomic test fails.
     *
     * @param failure describes the test that failed and the exception that was thrown
     */
    public void testFailure(Failure failure) throws Exception {
        TestCaseResult testCaseResult = runningCases.get(failure.getDescription().getDisplayName());
        Throwable t = failure.getException();
        if (t instanceof AssertionError || t instanceof AssertionFailedError) {
            suiteResult.incrementFailures();
            FailureResult failureResult = new FailureResult(
                    failure.getException().getClass(),
                    failure.getMessage(),
                    extractStackTrace(failure.getException())
            );
            testCaseResult.setFailure(failureResult);
        } else if (Modifier.isAbstract(failure.getDescription().getTestClass().getModifiers())) {
            suiteResult.incrementSkipped();
            testCaseResult.setSkip(true);
        } else {
            suiteResult.incrementErrors();
            ErrorResult errorResult = new ErrorResult(
                    failure.getException().getClass(),
                    failure.getMessage(),
                    extractStackTrace(failure.getException())
            );
            testCaseResult.setError(errorResult);
        }
    }

    /**
     * Called when an atomic test flags that it assumes a condition that is
     * false
     *
     * @param failure describes the test that failed and the
     * {@link org.junit.internal.AssumptionViolatedException} that was thrown
     */
    public void testAssumptionFailure(Failure failure) {
        suiteResult.incrementFailures();
        TestCaseResult testCaseResult = runningCases.get(failure.getDescription().getDisplayName());
        FailureResult failureResult = new FailureResult(
                failure.getException().getClass(),
                failure.getMessage(),
                extractStackTrace(failure.getException())
        );
        testCaseResult.setFailure(failureResult);
    }

    /**
     * Called when a test will not be run, generally because a test method is annotated
     * with {@link org.junit.Ignore}.
     *
     * @param description describes the test that will not be run
     */
    public void testIgnored(Description description) throws Exception {
        TestCaseResult testCaseResult = runningCases.get(description.getDisplayName());
        testCaseResult.setSkip(true);
        suiteResult.incrementSkipped();
    }
    /**
     * Called when all tests have finished
     *
     * @param result the summary of the test run, including all the tests that failed
     */
    public void testRunFinished(Result result) throws Exception {
        suiteResult.setTime((System.currentTimeMillis() - suiteStartedTime) / 1000f);
    }

}
