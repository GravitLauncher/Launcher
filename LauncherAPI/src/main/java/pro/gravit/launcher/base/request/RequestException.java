package pro.gravit.launcher.base.request;

import java.io.IOException;

public final class RequestException extends IOException {


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
