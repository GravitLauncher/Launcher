package pro.gravit.launchserver.auth.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.MySQLSourceConfig;
import pro.gravit.launchserver.auth.password.PasswordVerifier;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class MySQLCoreProvider extends AuthCoreProvider {
    private transient final Logger logger = LogManager.getLogger();
    public MySQLSourceConfig mySQLHolder;

    public String uuidColumn;
    public String usernameColumn;
    public String accessTokenColumn;
    public String passwordColumn;
    public String serverIDColumn;
    public String table;
    public PasswordVerifier passwordVerifier;

    // Prepared SQL queries
    private transient String queryByUUIDSQL;
    private transient String queryByUsernameSQL;
    private transient String updateAuthSQL;
    private transient String updateServerIDSQL;

    @Override
    public User getUserByUsername(String username) {
        try {
            return query(queryByUsernameSQL, username);
        } catch (IOException e) {
            logger.error("SQL error", e);
            return null;
        }
    }

    @Override
    public User getUserByUUID(UUID uuid) {
        try {
            return query(queryByUUIDSQL, uuid.toString());
        } catch (IOException e) {
            logger.error("SQL error", e);
            return null;
        }
    }

    @Override
    public User getUserByOAuthAccessToken(String accessToken) {
        return null;
    }

    @Override
    public AuthManager.AuthReport refreshAccessToken(String refreshToken, AuthResponse.AuthContext context) {
        return null;
    }

    @Override
    public void verifyAuth(AuthResponse.AuthContext context) throws AuthException {

    }

    @Override
    public PasswordVerifyReport verifyPassword(User user, AuthRequest.AuthPasswordInterface password) {
        if(passwordVerifier.check(((MySQLUser)user).password, ((AuthPlainPassword)password).password)) {
            return new PasswordVerifyReport(true);
        } else {
            return PasswordVerifyReport.FAILED;
        }
    }

    @Override
    public AuthManager.AuthReport createOAuthSession(User user, AuthResponse.AuthContext context, PasswordVerifyReport report, boolean minecraftAccess) throws IOException {
        if(minecraftAccess) {
            String minecraftAccessToken = SecurityHelper.randomStringToken();
            updateAuth(user, minecraftAccessToken);
            return AuthManager.AuthReport.ofMinecraftAccessToken(minecraftAccessToken);
        } else {
            return AuthManager.AuthReport.ofMinecraftAccessToken(null);
        }
    }

    @Override
    public void init(LaunchServer server) {
        if (mySQLHolder == null) logger.error("mySQLHolder cannot be null");
        if (uuidColumn == null) logger.error("uuidColumn cannot be null");
        if (usernameColumn == null) logger.error("usernameColumn cannot be null");
        if (accessTokenColumn == null) logger.error("accessTokenColumn cannot be null");
        if (serverIDColumn == null) logger.error("serverIDColumn cannot be null");
        if (table == null) logger.error("table cannot be null");
        // Prepare SQL queries
        queryByUUIDSQL = String.format("SELECT %s, %s, %s, %s, %s FROM %s WHERE %s=? LIMIT 1",
                uuidColumn, usernameColumn, accessTokenColumn, serverIDColumn, passwordColumn, table, uuidColumn);
        queryByUsernameSQL = String.format("SELECT %s, %s, %s, %s, %s FROM %s WHERE %s=? LIMIT 1",
                uuidColumn, usernameColumn, accessTokenColumn, serverIDColumn, passwordColumn, table, usernameColumn);

        updateAuthSQL = String.format("UPDATE %s SET %s=?, %s=NULL WHERE %s=? LIMIT 1",
                table, accessTokenColumn, serverIDColumn, uuidColumn);
        updateServerIDSQL = String.format("UPDATE %s SET %s=? WHERE %s=? LIMIT 1",
                table, serverIDColumn, uuidColumn);
    }

    protected boolean updateAuth(User user, String accessToken) throws IOException {
        try (Connection c = mySQLHolder.getConnection()) {
            PreparedStatement s = c.prepareStatement(updateAuthSQL);
            s.setString(1, accessToken);
            s.setString(2, user.getUUID().toString());
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            return s.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected boolean updateServerID(User user, String serverID) throws IOException {
        try (Connection c = mySQLHolder.getConnection()) {
            PreparedStatement s = c.prepareStatement(updateServerIDSQL);
            s.setString(1, serverID);
            s.setString(2, user.getUUID().toString());
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            return s.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        mySQLHolder.close();
    }

    private MySQLUser constructUser(ResultSet set) throws SQLException {
        return set.next() ? new MySQLUser(UUID.fromString(set.getString(uuidColumn)), set.getString(usernameColumn),
                set.getString(accessTokenColumn), set.getString(serverIDColumn), set.getString(passwordColumn), new ClientPermissions()) : null;
    }

    private User query(String sql, String value) throws IOException {
        try (Connection c = mySQLHolder.getConnection()) {
            PreparedStatement s = c.prepareStatement(sql);
            s.setString(1, value);
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            try (ResultSet set = s.executeQuery()) {
                return constructUser(set);
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    public static class MySQLUser implements User {
        private final UUID uuid;
        private final String username;
        private final String accessToken;
        private final String serverId;
        private final String password;
        private final ClientPermissions permissions;

        public MySQLUser(UUID uuid, String username, String accessToken, String serverId, String password, ClientPermissions permissions) {
            this.uuid = uuid;
            this.username = username;
            this.accessToken = accessToken;
            this.serverId = serverId;
            this.password = password;
            this.permissions = permissions;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public UUID getUUID() {
            return uuid;
        }

        @Override
        public String getServerId() {
            return serverId;
        }

        @Override
        public String getAccessToken() {
            return accessToken;
        }

        @Override
        public ClientPermissions getPermissions() {
            return permissions;
        }
    }
}
