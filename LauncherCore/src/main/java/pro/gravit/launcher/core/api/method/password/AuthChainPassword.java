package pro.gravit.launcher.core.api.method.password;

import pro.gravit.launcher.core.api.method.AuthMethodPassword;

import java.util.List;

public record AuthChainPassword(List<AuthMethodPassword> list) implements AuthMethodPassword {
}
