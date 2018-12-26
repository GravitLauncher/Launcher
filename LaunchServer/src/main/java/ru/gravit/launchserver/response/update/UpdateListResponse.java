package ru.gravit.launchserver.response.update;

import java.util.Map.Entry;
import java.util.Set;

import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.response.Response;

public final class UpdateListResponse extends Response {

    public UpdateListResponse(LaunchServer server, long session, HInput input, HOutput output, String ip) {
        super(server, session, input, output, ip);
    }

    @Override
    public void reply() throws Exception {
        Set<Entry<String, SignedObjectHolder<HashedDir>>> updateDirs = server.getUpdateDirs();
        // Write all update dirs names
        output.writeLength(updateDirs.size(), 0);
        for (Entry<String, SignedObjectHolder<HashedDir>> entry : updateDirs)
            output.writeString(entry.getKey(), 255);
    }
}
