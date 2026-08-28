package com.microx.engine.save;
public final class SaveException extends Exception {
    public static final int FAILURE = 0, EMPTY_SLOT = 1;
    private final Throwable cause;
    private final int kind;

    public SaveException(String message) {
        super(message);
        cause = null;
        kind = FAILURE;
    }
    public SaveException(String message, Throwable cause) {
        super(message);
        this.cause = cause;
        kind = FAILURE;
    }
    private SaveException(String message, int value) {
        super(message);
        cause = null;
        kind = value;
    }
    public static SaveException emptySlot() {
        return new SaveException("save slot is empty", EMPTY_SLOT);
    }
    public boolean isEmptySlot() {
        return kind == EMPTY_SLOT;
    }
    public Throwable getCause() {
        return cause;
    }
}
