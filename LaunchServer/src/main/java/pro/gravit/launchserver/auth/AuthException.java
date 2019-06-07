package pro.gravit.launchserver.auth;

import java.io.IOException;

public final class AuthException extends IOException {
    private static final long serialVersionUID = -2586107832847245863L;


    public AuthException(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
