package pro.gravit.launcher.request;

import java.io.IOException;

public final class RequestException extends IOException {
    private static final long serialVersionUID = 7558237657082664821L;


    public RequestException(String message) {
        super(message);
    }


    public RequestException(String message, Throwable exc) {
        super(message, exc);
    }


    public RequestException(Throwable exc) {
        super(exc);
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
