package ru.gravit.launchserver.auth.provider;

import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launcher.serialize.config.entry.BooleanConfigEntry;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.ClientPermissions;
import ru.gravit.utils.helper.SecurityHelper;

public final class AcceptAuthProvider extends AuthProvider {
    private final boolean isAdminAccess;

    public AcceptAuthProvider(BlockConfigEntry block, LaunchServer server) {
        super(block, server);
        isAdminAccess = block.hasEntry("admin") ? block.getEntryValue("admin", BooleanConfigEntry.class) : false;
    }

    @Override
    public AuthProviderResult auth(String login, String password, String ip) {
        return new AuthProviderResult(login, SecurityHelper.randomStringToken(), isAdminAccess ? ClientPermissions.getSuperuserAccount() : ClientPermissions.DEFAULT); // Same as login
    }

    @Override
    public void close() {
        // Do nothing
    }
}
