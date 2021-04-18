package pro.gravit.launchserver.auth.provider;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.MySQLSourceConfig;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class MySQLAuthProvider extends AuthProvider {
    private MySQLSourceConfig mySQLHolder;
    private String query;
    private String message;
    private String[] queryParams;
    private boolean flagsEnabled;

    @Override
    public void init(LaunchServer srv) {
        super.init(srv);
        if (query == null) throw new RuntimeException("[Verify][AuthProvider] query cannot be null");
        if (message == null) throw new RuntimeException("[Verify][AuthProvider] message cannot be null");
        if (mySQLHolder == null) throw new RuntimeException("[Verify][AuthProvider] mySQLHolder cannot be null");
    }

    @Override
    public AuthProviderResult auth(String login, AuthRequest.AuthPasswordInterface password, String ip) throws SQLException, AuthException {
        if (!(password instanceof AuthPlainPassword)) throw new AuthException("This password type not supported");
        try (Connection c = mySQLHolder.getConnection()) {
            PreparedStatement s = c.prepareStatement(query);
            String[] replaceParams = {"login", login, "password", ((AuthPlainPassword) password).password, "ip", ip};
            for (int i = 0; i < queryParams.length; i++)
                s.setString(i + 1, CommonHelper.replace(queryParams[i], replaceParams));

            // Execute SQL query
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            try (ResultSet set = s.executeQuery()) {
                return set.next() ? new AuthProviderResult(set.getString(1), SecurityHelper.randomStringToken(), new ClientPermissions(
                        set.getLong(2), flagsEnabled ? set.getLong(3) : 0)) : authError(message);
            }
        }

    }

    @Override
    public void close() {
        mySQLHolder.close();
    }
}
