package ru.gravit.launcher.request.auth;

import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.events.request.SetProfileRequestEvent;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;

public class SetProfileRequest extends Request<SetProfileRequestEvent> implements RequestInterface {
    private transient ClientProfile profile;

    public String client;

    public SetProfileRequest(LauncherConfig config, ClientProfile profile) {
        super(config);
        this.profile = profile;
        this.client = profile.getTitle();
    }
    @Override
    public SetProfileRequestEvent requestWebSockets() throws Exception
    {
        return (SetProfileRequestEvent) LegacyRequestBridge.sendRequest(this);
    }
    @Override
    public Integer getLegacyType() {
        return RequestType.SETPROFILE.getNumber();
    }

    @Override
    protected SetProfileRequestEvent requestDo(HInput input, HOutput output) throws Exception {
        output.writeString(profile.getTitle(), SerializeLimits.MAX_CLIENT);
        readError(input);
        return new SetProfileRequestEvent(profile);
    }

    @Override
    public String getType() {
        return "setProfile";
    }
}
