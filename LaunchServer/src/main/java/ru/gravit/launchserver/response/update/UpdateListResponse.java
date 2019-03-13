package ru.gravit.launchserver.response.update;

import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.response.Response;
import ru.gravit.launchserver.socket.Client;

import java.util.Map.Entry;
import java.util.Set;

public final class UpdateListResponse extends Response {

    public UpdateListResponse(LaunchServer server, long session, HInput input, HOutput output, String ip, Client clientData) {
        super(server, session, input, output, ip, clientData);
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
