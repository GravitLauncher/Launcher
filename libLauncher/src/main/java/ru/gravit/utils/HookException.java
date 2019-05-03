package ru.gravit.utils;

public class HookException extends RuntimeException {
    public HookException() {
    }

    public HookException(String message) {
        super(message);
    }

    public HookException(String message, Throwable cause) {
        super(message, cause);
    }

    public HookException(Throwable cause) {
        super(cause);
    }

    public HookException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
