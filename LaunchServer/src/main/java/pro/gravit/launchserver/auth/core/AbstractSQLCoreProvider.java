package pro.gravit.launchserver.auth.core;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.MySQLSourceConfig;
import pro.gravit.launchserver.auth.SQLSourceConfig;
import pro.gravit.launchserver.auth.password.PasswordVerifier;
import pro.gravit.launchserver.helper.LegacySessionHelper;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.sql.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractSQLCoreProvider extends AuthCoreProvider {
    public transient Logger logger = LogManager.getLogger();

    public abstract SQLSourceConfig getSQLConfig();

    public int expireSeconds = 3600;
    public String uuidColumn;
    public String usernameColumn;
    public String accessTokenColumn;
    public String passwordColumn;
    public String serverIDColumn;
    public String table;

    public String permissionsTable;
    public String permissionsPermissionColumn;
    public String permissionsUUIDColumn;

    public String rolesTable;
    public String rolesNameColumn;
    public String rolesUUIDColumn;

    public PasswordVerifier passwordVerifier;
    public String customQueryByUUIDSQL;
    public String customQueryByUsernameSQL;
    public String customQueryByLoginSQL;
    public String customQueryPermissionsByUUIDSQL;
    public String customQueryRoleByUUIDSQL;
    public String customQueryRoleByNameSQL;
    public String customUpdateAuthSQL;
    public String customUpdateServerIdSQL;
    // Prepared SQL queries
    public transient String queryByUUIDSQL;
    public transient String queryByUsernameSQL;
    public transient String queryByLoginSQL;
    public transient String queryPermissionsByUUIDSQL;
    public transient String queryRoleByUUIDSQL;
    public transient String queryRoleByNameSQL;
    public transient String updateAuthSQL;
    public transient String updateServerIDSQL;

    public transient LaunchServer server;

    @Override
    public User getUserByUsername(String username) {
        try {
            return queryUser(queryByUsernameSQL, username);
        } catch (Exception e) {
            logger.error("SQL error", e);
            return null;
        }
    }

    @Override
    public User getUserByUUID(UUID uuid) {
        try {
            return queryUser(queryByUUIDSQL, uuid.toString());
        } catch (Exception e) {
            logger.error("SQL error", e);
            return null;
        }
    }

    @Override
    public User getUserByLogin(String login) {
        try {
            return queryUser(queryByLoginSQL, login);
        } catch (Exception e) {
            logger.error("SQL error", e);
            return null;
        }
    }

    @Override
    public UserSession getUserSessionByOAuthAccessToken(String accessToken) throws OAuthAccessTokenExpired {
        try {
            var info = LegacySessionHelper.getJwtInfoFromAccessToken(accessToken, server.keyAgreementManager.ecdsaPublicKey);
            var user = (SQLUser) getUserByUUID(info.uuid());
            if(user == null) {
                return null;
            }
            return new SQLUserSession(user);
        } catch (ExpiredJwtException e) {
            throw new OAuthAccessTokenExpired();
        } catch (JwtException e) {
            return null;
        }
    }

    @Override
    public AuthManager.AuthReport refreshAccessToken(String refreshToken, AuthResponse.AuthContext context) {
        String[] parts = refreshToken.split("\\.");
        if(parts.length != 2) {
            return null;
        }
        String username = parts[0];
        String token = parts[1];
        var user = (SQLUser) getUserByUsername(username);
        if(user == null || user.password == null) {
            return null;
        }
        var realToken = LegacySessionHelper.makeRefreshTokenFromPassword(username, user.password, server.keyAgreementManager.legacySalt);
        if(!token.equals(realToken)) {
            return null;
        }
        var accessToken = LegacySessionHelper.makeAccessJwtTokenFromString(user, LocalDateTime.now(Clock.systemUTC()).plusSeconds(expireSeconds), server.keyAgreementManager.ecdsaPrivateKey);
        return new AuthManager.AuthReport(null, accessToken, refreshToken, expireSeconds * 1000L, new SQLUserSession(user));
    }

    @Override
    public AuthManager.AuthReport authorize(String login, AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password, boolean minecraftAccess) throws IOException {
        SQLUser SQLUser = (SQLUser) getUserByLogin(login);
        if(SQLUser == null) {
            throw AuthException.wrongPassword();
        }
        if(context != null) {
            AuthPlainPassword plainPassword = (AuthPlainPassword) password;
            if(plainPassword == null) {
                throw AuthException.wrongPassword();
            }
            if(!passwordVerifier.check(SQLUser.password, plainPassword.password)) {
                throw AuthException.wrongPassword();
            }
        }
        SQLUserSession session = new SQLUserSession(SQLUser);
        var accessToken = LegacySessionHelper.makeAccessJwtTokenFromString(SQLUser, LocalDateTime.now(Clock.systemUTC()).plusSeconds(expireSeconds), server.keyAgreementManager.ecdsaPrivateKey);
        var refreshToken = SQLUser.username.concat(".").concat(LegacySessionHelper.makeRefreshTokenFromPassword(SQLUser.username, SQLUser.password, server.keyAgreementManager.legacySalt));
        if (minecraftAccess) {
            String minecraftAccessToken = SecurityHelper.randomStringToken();
            updateAuth(SQLUser, minecraftAccessToken);
            return AuthManager.AuthReport.ofOAuthWithMinecraft(minecraftAccessToken, accessToken, refreshToken, expireSeconds * 1000L, session);
        } else {
            return AuthManager.AuthReport.ofOAuth(accessToken, refreshToken, expireSeconds * 1000L, session);
        }
    }

    @Override
    public void init(LaunchServer server) {
        this.server = server;
        if (getSQLConfig() == null) logger.error("SQLHolder cannot be null");
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

        queryPermissionsByUUIDSQL = customQueryPermissionsByUUIDSQL != null ? customQueryPermissionsByUUIDSQL : String.format("SELECT (%s) FROM %s WHERE %s=?",
                permissionsPermissionColumn, permissionsTable, permissionsUUIDColumn);

        queryRoleByUUIDSQL = customQueryRoleByUUIDSQL != null ? customQueryRoleByUUIDSQL : String.format("SELECT %s, %s FROM %s WHERE %s=? LIMIT 1",
                rolesUUIDColumn, rolesNameColumn,rolesTable,rolesUUIDColumn);

        queryRoleByNameSQL = customQueryRoleByNameSQL != null ? customQueryRoleByNameSQL : String.format("SELECT %s, %s FROM %s WHERE %s=? LIMIT 1",
                rolesUUIDColumn, rolesNameColumn,rolesTable,rolesNameColumn);

        updateAuthSQL = customUpdateAuthSQL != null ? customUpdateAuthSQL : String.format("UPDATE %s SET %s=?, %s=NULL WHERE %s=?",
                table, accessTokenColumn, serverIDColumn, uuidColumn);
        updateServerIDSQL = customUpdateServerIdSQL != null ? customUpdateServerIdSQL : String.format("UPDATE %s SET %s=? WHERE %s=?",
                table, serverIDColumn, uuidColumn);
    }

    protected boolean updateAuth(User user, String accessToken) throws IOException {
        try (Connection c = getSQLConfig().getConnection()) {
            SQLUser SQLUser = (SQLUser) user;
            SQLUser.accessToken = accessToken;
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
        try (Connection c = getSQLConfig().getConnection()) {
            SQLUser SQLUser = (SQLUser) user;
            SQLUser.serverId = serverID;
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
        getSQLConfig().close();
    }

    private SQLUser constructUser(ResultSet set) throws SQLException {
        return set.next() ? new SQLUser(UUID.fromString(set.getString(uuidColumn)), set.getString(usernameColumn),
                set.getString(accessTokenColumn), set.getString(serverIDColumn), set.getString(passwordColumn), requestPermissions(set.getString(uuidColumn))) : null;
    }

    public ClientPermissions requestPermissions (String uuid)  throws SQLException
    {
        try{
            ClientPermissions perms = new ClientPermissions();

            for(String perm : queryPermissions(queryPermissionsByUUIDSQL,uuid))
                perms.addPerm(perm);

            List<String> groupPerms = new ArrayList<>(perms.getPerms());
            groupPerms.removeIf(s->!s.startsWith("role."));

            for(String groupPerm : groupPerms)
            {
                String role = groupPerm.substring(5);
                if(processRole(queryRole(queryRoleByUUIDSQL,role),perms)) continue;
                if(processRole(queryRole(queryRoleByNameSQL,role),perms)) continue;
                logger.warn(String.format( "Role not found: %s",role));
            }

            return perms;
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    private boolean processRole(SQLRole role, ClientPermissions perms) throws SQLException {
        if(role == null) return false;
        perms.addRole(role.name);
        for(String perm : queryPermissions(queryPermissionsByUUIDSQL,role.uuid))
            perms.addPerm(perm);
        return true;
    }

    private SQLUser queryUser(String sql, String value) throws SQLException {
        try (Connection c = getSQLConfig().getConnection()) {
            PreparedStatement s = c.prepareStatement(sql);
            s.setString(1, value);
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            try (ResultSet set = s.executeQuery()) {
                return constructUser(set);
            }
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    private List<String> queryPermissions(String sql, String value) throws SQLException {
        try (Connection c = getSQLConfig().getConnection()) {
            PreparedStatement s = c.prepareStatement(sql);
            s.setString(1, value);
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            ResultSet set = s.executeQuery();
            List<String> perms = new ArrayList<>();
            while (set.next())
                perms.add(set.getString(permissionsPermissionColumn));
            return perms;
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    private SQLRole queryRole(String sql, String value) throws SQLException {
        try (Connection c = getSQLConfig().getConnection()) {
            PreparedStatement s = c.prepareStatement(sql);
            s.setString(1, value);
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            ResultSet set = s.executeQuery();
            if(!set.next()) return null;
            return new SQLRole(set.getString(rolesNameColumn), set.getString(rolesUUIDColumn));
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    private class SQLRole
    {
        public String name;
        public String uuid;

        SQLRole(String name, String uuid)
        {
            this.name = name;
            this.uuid = uuid;
        }
    }

    public static class SQLUser implements User {
        protected UUID uuid;
        protected String username;
        protected String accessToken;
        protected String serverId;
        protected String password;
        protected ClientPermissions permissions;

        public SQLUser(UUID uuid, String username, String accessToken, String serverId, String password, ClientPermissions permissions) {
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
            return "SQLUser{" +
                    "uuid=" + uuid +
                    ", username='" + username + '\'' +
                    ", permissions=" + permissions +
                    '}';
        }
    }

    public static class SQLUserSession implements UserSession {
        private final SQLUser user;
        private final String id;

        public SQLUserSession(SQLUser user) {
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
