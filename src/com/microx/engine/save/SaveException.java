package com.microx.engine.save;
public final class SaveException extends Exception {
    private final Throwable cause;

    public SaveException(String message) {
        super(message);
        cause = null;
    }
    public SaveException(String message, Throwable cause) {
        super(message);
        this.cause = cause;
    }
    public Throwable getCause() {
        return cause;
    }
}
