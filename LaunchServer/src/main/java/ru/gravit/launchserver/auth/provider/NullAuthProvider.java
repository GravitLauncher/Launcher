package ru.gravit.launchserver.auth.provider;

import java.io.IOException;
import java.util.Objects;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;

public final class NullAuthProvider extends AuthProvider {
    private volatile AuthProvider provider;

    public NullAuthProvider(BlockConfigEntry block, LaunchServer server) {
        super(block,server);
    }

    @Override
    public AuthProviderResult auth(String login, String password, String ip) throws Exception {
        return getProvider().auth(login, password, ip);
    }

    @Override
    public void close() throws IOException {
        AuthProvider provider = this.provider;
        if (provider != null)
            provider.close();
    }

    private AuthProvider getProvider() {
        return VerifyHelper.verify(provider, Objects::nonNull, "Backend auth provider wasn't set");
    }


    public void setBackend(AuthProvider provider) {
        this.provider = provider;
    }
}
