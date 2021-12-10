package pro.gravit.launchserver.auth.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.MySQLSourceConfig;
import pro.gravit.launchserver.auth.PostgreSQLSourceConfig;
import pro.gravit.launchserver.auth.password.PasswordVerifier;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.sql.*;
import java.util.UUID;

public class PostgresSQLCoreProvider extends AuthCoreProvider {
    private transient final Logger logger = LogManager.getLogger();
    public PostgreSQLSourceConfig postgresSQLHolder;

    public String uuidColumn;
    public String usernameColumn;
    public String accessTokenColumn;
    public String passwordColumn;
    public String serverIDColumn;
    public String table;

    public PasswordVerifier passwordVerifier;
    public String customQueryByUUIDSQL;
    public String customQueryByUsernameSQL;
    public String customQueryByLoginSQL;
    public String customUpdateAuthSQL;
    public String customUpdateServerIdSQL;
    // Prepared SQL queries
    private transient String queryByUUIDSQL;
    private transient String queryByUsernameSQL;
    private transient String queryByLoginSQL;
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
    public User getUserByLogin(String login) {
        try {
            return query(queryByLoginSQL, login);
        } catch (IOException e) {
            logger.error("SQL error", e);
            return null;
        }
    }

    @Override
    public UserSession getUserSessionByOAuthAccessToken(String accessToken) throws OAuthAccessTokenExpired {
        return null;
    }

    @Override
    public AuthManager.AuthReport refreshAccessToken(String refreshToken, AuthResponse.AuthContext context) {
        return null;
    }

    @Override
    public AuthManager.AuthReport authorize(String login, AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password, boolean minecraftAccess) throws IOException {
        PostgresSQLUser postgresSQLUser = (PostgresSQLUser) getUserByLogin(login);
        if(postgresSQLUser == null) {
            throw AuthException.wrongPassword();
        }
        if(context != null) {
            AuthPlainPassword plainPassword = (AuthPlainPassword) password;
            if(plainPassword == null) {
                throw AuthException.wrongPassword();
            }
            if(!passwordVerifier.check(postgresSQLUser.password, plainPassword.password)) {
                throw AuthException.wrongPassword();
            }
        }
        MySQLUserSession session = new MySQLUserSession(postgresSQLUser);
        if (minecraftAccess) {
            String minecraftAccessToken = SecurityHelper.randomStringToken();
            updateAuth(postgresSQLUser, minecraftAccessToken);
            return AuthManager.AuthReport.ofMinecraftAccessToken(minecraftAccessToken, session);
        } else {
            return AuthManager.AuthReport.ofMinecraftAccessToken(null, session);
        }
    }

    @Override
    public void init(LaunchServer server) {
        if (postgresSQLHolder == null) logger.error("postgresSQLHolder cannot be null");
        if (uuidColumn == null) logger.error("uuidColumn cannot be null");
        if (usernameColumn == null) logger.error("usernameColumn cannot be null");
        if (accessTokenColumn == null) logger.error("accessTokenColumn cannot be null");
        if (serverIDColumn == null) logger.error("serverIDColumn cannot be null");
        if (table == null) logger.error("table cannot be null");
        // Prepare SQL queries
        String userInfoCols = String.format("%s, %s, %s, %s, %s", uuidColumn, usernameColumn, accessTokenColumn, serverIDColumn, passwordColumn);
        queryByUUIDSQL = customQueryByUUIDSQL != null ? customQueryByUUIDSQL : String.format("SELECT %s FROM %s WHERE %s=? LIMIT 1", userInfoCols,
                table, uuidColumn);
        queryByUsernameSQL = customQueryByUsernameSQL != null ? customQueryByUsernameSQL : String.format("SELECT %s FROM %s WHERE %s=? LIMIT 1",
                userInfoCols, table, usernameColumn);
        queryByLoginSQL = customQueryByLoginSQL != null ? customQueryByLoginSQL : queryByUsernameSQL;

        updateAuthSQL = customUpdateAuthSQL != null ? customUpdateAuthSQL : String.format("UPDATE %s SET %s=?, %s=NULL WHERE %s=?",
                table, accessTokenColumn, serverIDColumn, uuidColumn);
        updateServerIDSQL = customUpdateServerIdSQL != null ? customUpdateServerIdSQL : String.format("UPDATE %s SET %s=? WHERE %s=?",
                table, serverIDColumn, uuidColumn);
    }

    protected boolean updateAuth(User user, String accessToken) throws IOException {
        try (Connection c = postgresSQLHolder.getConnection()) {
            PostgresSQLUser postgresSQLUser = (PostgresSQLUser) user;
            postgresSQLUser.accessToken = accessToken;
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
        try (Connection c = postgresSQLHolder.getConnection()) {
            PostgresSQLUser postgresSQLUser = (PostgresSQLUser) user;
            postgresSQLUser.serverId = serverID;
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
        postgresSQLHolder.close();
    }

    private PostgresSQLUser constructUser(ResultSet set) throws SQLException {
        return set.next() ? new PostgresSQLUser(UUID.fromString(set.getString(uuidColumn)), set.getString(usernameColumn),
                set.getString(accessTokenColumn), set.getString(serverIDColumn), set.getString(passwordColumn), new ClientPermissions()) : null;
    }

    private User query(String sql, String value) throws IOException {
        try (Connection c = postgresSQLHolder.getConnection()) {
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

    public static class PostgresSQLUser implements User {
        protected UUID uuid;
        protected String username;
        protected String accessToken;
        protected String serverId;
        protected String password;
        protected ClientPermissions permissions;

        public PostgresSQLUser(UUID uuid, String username, String accessToken, String serverId, String password, ClientPermissions permissions) {
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

        @Override
        public String toString() {
            return "PostgresSQLUser{" +
                    "uuid=" + uuid +
                    ", username='" + username + '\'' +
                    ", permissions=" + permissions +
                    '}';
        }
    }

    public static class MySQLUserSession implements UserSession {
        private final PostgresSQLUser user;
        private final String id;

        public MySQLUserSession(PostgresSQLUser user) {
            this.user = user;
            this.id = user.username;
        }

        @Override
        public String getID() {
            return id;
        }

        @Override
        public User getUser() {
            return user;
        }

        @Override
        public long getExpireIn() {
            return 0;
        }
    }
}
