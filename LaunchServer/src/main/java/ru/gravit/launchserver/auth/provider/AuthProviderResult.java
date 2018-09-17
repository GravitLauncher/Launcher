package ru.gravit.launchserver.auth.provider;

public class AuthProviderResult {
    public final String username;
    public final String accessToken;

    public AuthProviderResult(String username, String accessToken) {
        this.username = username;
        this.accessToken = accessToken;
    }
}
