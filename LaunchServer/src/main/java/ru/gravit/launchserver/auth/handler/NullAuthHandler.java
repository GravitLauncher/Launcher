package ru.gravit.launchserver.auth.handler;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launchserver.auth.provider.AuthProviderResult;
import ru.gravit.utils.helper.VerifyHelper;

public final class NullAuthHandler extends AuthHandler {
    private volatile AuthHandler handler;

    public NullAuthHandler(BlockConfigEntry block) {
        super(block);
    }

    @Override
    public UUID auth(AuthProviderResult authResult) throws IOException {
        return getHandler().auth(authResult);
    }

    @Override
    public UUID checkServer(String username, String serverID) throws IOException {
        return getHandler().checkServer(username, serverID);
    }

    @Override
    public void close() throws IOException {
        AuthHandler handler = this.handler;
        if (handler != null)
            handler.close();
    }

    private AuthHandler getHandler() {
        return VerifyHelper.verify(handler, Objects::nonNull, "Backend auth handler wasn't set");
    }

    @Override
    public boolean joinServer(String username, String accessToken, String serverID) throws IOException {
        return getHandler().joinServer(username, accessToken, serverID);
    }


    public void setBackend(AuthHandler handler) {
        this.handler = handler;
    }

    @Override
    public UUID usernameToUUID(String username) throws IOException {
        return getHandler().usernameToUUID(username);
    }

    @Override
    public String uuidToUsername(UUID uuid) throws IOException {
        return getHandler().uuidToUsername(uuid);
    }
}
