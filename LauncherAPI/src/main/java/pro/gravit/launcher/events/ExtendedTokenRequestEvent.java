package pro.gravit.launcher.events;

public interface ExtendedTokenRequestEvent {
    String getExtendedTokenName();

    String getExtendedToken();

    long getExtendedTokenExpire();
}
