package com.microx.engine.save;
public final class SaveException extends Exception {
    public SaveException(String message) {
        super(message);
    }
    public SaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
