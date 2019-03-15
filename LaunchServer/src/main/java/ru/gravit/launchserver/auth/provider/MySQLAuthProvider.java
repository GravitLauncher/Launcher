package ru.gravit.launchserver.auth.provider;

import ru.gravit.launcher.ClientPermissions;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.auth.MySQLSourceConfig;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class MySQLAuthProvider extends AuthProvider {
    private MySQLSourceConfig mySQLHolder;
    private String query;
    private String message;
    private String[] queryParams;
    private boolean usePermission;

    @Override
    public void init() {
        if (query == null) LogHelper.error("[Verify][AuthProvider] query cannot be null");
        if (message == null) LogHelper.error("[Verify][AuthProvider] message cannot be null");
        if (mySQLHolder == null) LogHelper.error("[Verify][AuthProvider] mySQLHolder cannot be null");
    }

    @Override
    public AuthProviderResult auth(String login, String password, String ip) throws SQLException, AuthException {
        try(Connection c = mySQLHolder.getConnection())
        {
            PreparedStatement s = c.prepareStatement(query);
            String[] replaceParams = {"login", login, "password", password, "ip", ip};
            for (int i = 0; i < queryParams.length; i++)
                s.setString(i + 1, CommonHelper.replace(queryParams[i], replaceParams));

            // Execute SQL query
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            try (ResultSet set = s.executeQuery()) {
                return set.next() ? new AuthProviderResult(set.getString(1), SecurityHelper.randomStringToken(), usePermission ? new ClientPermissions(set.getLong(2)) : LaunchServer.server.config.permissionsHandler.getPermissions(set.getString(1))) : authError(message);
            }
        }

    }

    @Override
    public void close() {
    	mySQLHolder.close();
    }
}
