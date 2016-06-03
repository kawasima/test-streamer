package test_streamer.client.dto;

import us.bpsm.edn.Symbol;

/**
 * @author kawasima
 */
public class ErrorResult {
    private Symbol type;
    private String message;
    private String stacktrace;

    public ErrorResult(Class clazz, String message, String stacktrace) {
        this.type = Symbol.newSymbol(clazz.getName());
        this.message = message;
        this.stacktrace = stacktrace;
    }

    public Symbol getType() {
        return type;
    }

    public void setType(Symbol type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    public void setStacktrace(String stacktrace) {
        this.stacktrace = stacktrace;
    }
}
