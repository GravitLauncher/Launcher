package pro.gravit.launcher.base.events;

public interface ExtendedTokenRequestEvent {
    String getExtendedTokenName();

    String getExtendedToken();

    long getExtendedTokenExpire();
}
