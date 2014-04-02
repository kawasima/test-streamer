package test_streamer.client.dto;

import test_streamer.client.PropertyName;

/**
 * @author kawasima

 */
public class TestCaseResult {
    private String classname;
    private String name;
    private float time;

    private boolean skip;
    private FailureResult failure;
    private ErrorResult error;

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }

    @PropertyName("skip?")
    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public FailureResult getFailure() {
        return failure;
    }

    public void setFailure(FailureResult failure) {
        this.failure = failure;
    }

    public ErrorResult getError() {
        return error;
    }

    public void setError(ErrorResult error) {
        this.error = error;
    }
}
