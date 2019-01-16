package ru.gravit.launcher.request.update;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ProfilesRequest extends Request<ProfilesRequest.Result> {

    public static final class Result {
        @LauncherAPI
        public final List<ClientProfile> profiles;

        private Result(List<ClientProfile> profiles) {
            this.profiles = Collections.unmodifiableList(profiles);
        }
    }

    @LauncherAPI
    public ProfilesRequest() {
        this(null);
    }

    @LauncherAPI
    public ProfilesRequest(LauncherConfig config) {
        super(config);
    }

    @Override
    public Integer getType() {
        return RequestType.PROFILES.getNumber();
    }

    @Override
    protected Result requestDo(HInput input, HOutput output) throws Exception {
        output.writeBoolean(true);
        output.flush();
        readError(input);

        int count = input.readLength(0);
        List<ClientProfile> profiles = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String prof = input.readString(0);
            profiles.add(Launcher.gson.fromJson(prof, ClientProfile.class));
        }
        // Return request result
        return new Result(profiles);
    }
}
