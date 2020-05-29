package pro.gravit.launchserver.auth.protect.hwid;

public class HWIDException extends Exception {
    public HWIDException() {
    }

    public HWIDException(String message) {
        super(message);
    }

    public HWIDException(String message, Throwable cause) {
        super(message, cause);
    }

    public HWIDException(Throwable cause) {
        super(cause);
    }

    public HWIDException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
