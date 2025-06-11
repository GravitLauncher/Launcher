package pro.gravit.launcher.core.api.method.password;

import pro.gravit.launcher.core.api.method.AuthMethodPassword;

public record AuthOAuthPassword(String redirectUrl) implements AuthMethodPassword {
}
