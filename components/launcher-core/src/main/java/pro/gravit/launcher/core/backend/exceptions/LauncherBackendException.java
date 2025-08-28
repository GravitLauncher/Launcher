package pro.gravit.launcher.core.backend.exceptions;

public class LauncherBackendException extends RuntimeException {
    public LauncherBackendException() {
    }

    public LauncherBackendException(String message) {
        super(message);
    }

    public LauncherBackendException(String message, Throwable cause) {
        super(message, cause);
    }
}
