package pro.gravit.launchserver.auth.provider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.MySQLSourceConfig;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

public final class MySQLAuthProvider extends AuthProvider {
    private MySQLSourceConfig mySQLHolder;
    private String query;
    private String message;
    private String[] queryParams;
    private boolean usePermission;

    @Override
    public void init(LaunchServer srv) {
    	super.init(srv);
        if (query == null) LogHelper.error("[Verify][AuthProvider] query cannot be null");
        if (message == null) LogHelper.error("[Verify][AuthProvider] message cannot be null");
        if (mySQLHolder == null) LogHelper.error("[Verify][AuthProvider] mySQLHolder cannot be null");
    }

    @Override
    public AuthProviderResult auth(String login, String password, String ip) throws SQLException, AuthException {
        try (Connection c = mySQLHolder.getConnection()) {
            PreparedStatement s = c.prepareStatement(query);
            String[] replaceParams = {"login", login, "password", password, "ip", ip};
            for (int i = 0; i < queryParams.length; i++)
                s.setString(i + 1, CommonHelper.replace(queryParams[i], replaceParams));

            // Execute SQL query
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            try (ResultSet set = s.executeQuery()) {
                return set.next() ? new AuthProviderResult(set.getString(1), SecurityHelper.randomStringToken(), usePermission ? new ClientPermissions(set.getLong(2)) : srv.config.permissionsHandler.getPermissions(set.getString(1))) : authError(message);
            }
        }

    }

    @Override
    public void close() {
        mySQLHolder.close();
    }
}
