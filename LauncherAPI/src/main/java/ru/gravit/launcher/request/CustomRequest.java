package ru.gravit.launcher.request;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.utils.helper.VerifyHelper;

public abstract class CustomRequest<T> extends Request<T> {
    @LauncherAPI
    public CustomRequest() {
        this(null);
    }

    @LauncherAPI
    public CustomRequest(LauncherConfig config) {
        super(config);
    }

    @LauncherAPI
    public abstract String getName();

    @Override
    public final Integer getLegacyType() {
        return 255;
    }

    @Override
    protected final T requestDo(HInput input, HOutput output) throws Exception {
        output.writeASCII(VerifyHelper.verifyIDName(getName()), 255);
        output.flush();

        // Custom request redirect
        return requestDoCustom(input, output);
    }

    @LauncherAPI
    protected abstract T requestDoCustom(HInput input, HOutput output);
}
