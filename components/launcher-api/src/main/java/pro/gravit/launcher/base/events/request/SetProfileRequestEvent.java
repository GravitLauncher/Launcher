package pro.gravit.launcher.base.events.request;

import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.base.events.ExtendedTokenRequestEvent;
import pro.gravit.launcher.base.events.RequestEvent;
import pro.gravit.launcher.base.profiles.ClientProfile;

import java.util.UUID;


public class SetProfileRequestEvent extends RequestEvent implements ExtendedTokenRequestEvent {
    public static final String CLIENT_PROFILE_EXTENDED_TOKEN_NAME = "clientProfile";
    @SuppressWarnings("unused")
    private static final UUID uuid = UUID.fromString("08c0de9e-4364-4152-9066-8354a3a48541");
    @LauncherNetworkAPI
    public final ClientProfile newProfile;
    @LauncherNetworkAPI
    public final String tag;
    public final String profileExtendedToken;
    public final long profileExtendedTokenExpire;

    public SetProfileRequestEvent(ClientProfile newProfile) {
        this(newProfile, null, null, 0);
    }

    public SetProfileRequestEvent(ClientProfile newProfile, String tag) {
        this(newProfile, tag, null, 0);
    }

    public SetProfileRequestEvent(ClientProfile newProfile, String tag, String profileExtendedToken, long profileExtendedTokenExpire) {
        this.newProfile = newProfile;
        this.tag = tag;
        this.profileExtendedToken = profileExtendedToken;
        this.profileExtendedTokenExpire = profileExtendedTokenExpire;
    }

    @Override
    public String getType() {
        return "setProfile";
    }

    @Override
    public String getExtendedTokenName() {
        return CLIENT_PROFILE_EXTENDED_TOKEN_NAME;
    }

    @Override
    public String getExtendedToken() {
        return profileExtendedToken;
    }

    @Override
    public long getExtendedTokenExpire() {
        return profileExtendedTokenExpire;
    }
}
