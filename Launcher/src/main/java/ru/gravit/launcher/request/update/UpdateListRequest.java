package ru.gravit.launcher.request.update;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.helper.IOHelper;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;

public final class UpdateListRequest extends Request<Set<String>> {
    @LauncherAPI
    public UpdateListRequest() {
        this(null);
    }

    @LauncherAPI
    public UpdateListRequest(LauncherConfig config) {
        super(config);
    }

    @Override
    public Integer getType() {
        return RequestType.UPDATE_LIST.getNumber();
    }

    @Override
    protected Set<String> requestDo(HInput input, HOutput output) throws IOException {
        int count = input.readLength(0);

        // Read all update dirs names
        Set<String> result = new HashSet<>(count);
        for (int i = 0; i < count; i++)
			result.add(IOHelper.verifyFileName(input.readString(255)));

        // We're done. Make it unmodifiable and return
        return Collections.unmodifiableSet(result);
    }
}
