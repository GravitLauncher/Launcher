package pro.gravit.launcher.base.vfs;

public class VfsException extends RuntimeException {
    public VfsException() {
    }

    public VfsException(String message) {
        super(message);
    }

    public VfsException(String message, Throwable cause) {
        super(message, cause);
    }

    public VfsException(Throwable cause) {
        super(cause);
    }

    public VfsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
