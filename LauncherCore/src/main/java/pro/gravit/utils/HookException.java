package pro.gravit.utils;

public class HookException extends RuntimeException {
    private static final long serialVersionUID = -529141998961943161L;

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
