package pro.gravit.launchserver.auth;

import pro.gravit.launcher.events.request.AuthRequestEvent;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public final class AuthException extends IOException {
    private static final long serialVersionUID = -2586107832847245863L;


    public AuthException(String message) {
        super(message);
    }

    public static AuthException need2FA() {
        return new AuthException(AuthRequestEvent.TWO_FACTOR_NEED_ERROR_MESSAGE);
    }

    public static AuthException needMFA(List<Integer> factors) {
        String message = AuthRequestEvent.ONE_FACTOR_NEED_ERROR_MESSAGE_PREFIX
                .concat(factors.stream().map(String::valueOf).collect(Collectors.joining(".")));
        return new AuthException(message);
    }

    public static AuthException wrongPassword() {
        return new AuthException(AuthRequestEvent.WRONG_PASSWORD_ERROR_MESSAGE);
    }

    public static AuthException userNotFound() {
        return new AuthException(AuthRequestEvent.USER_NOT_FOUND_ERROR_MESSAGE);
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
