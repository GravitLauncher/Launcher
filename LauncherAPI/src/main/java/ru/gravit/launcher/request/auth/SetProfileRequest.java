package ru.gravit.launcher.request.auth;

import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;

public class SetProfileRequest extends Request<Boolean> {
    private ClientProfile profile;

    public SetProfileRequest(LauncherConfig config, ClientProfile profile) {
        super(config);
        this.profile = profile;
    }

    @Override
    public Integer getType() {
        return RequestType.SETPROFILE.getNumber();
    }

    @Override
    protected Boolean requestDo(HInput input, HOutput output) throws Exception {
        output.writeString(profile.getTitle(), SerializeLimits.MAX_CLIENT);
        readError(input);
        return input.readBoolean();
    }
}
