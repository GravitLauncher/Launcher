package pro.gravit.launchserver.auth.core;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.base.request.auth.AuthRequest;
import pro.gravit.launcher.base.request.auth.password.AuthPlainPassword;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.MySQLSourceConfig;
import pro.gravit.launchserver.auth.SQLSourceConfig;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportSudo;
import pro.gravit.launchserver.auth.password.PasswordVerifier;
import pro.gravit.launchserver.helper.LegacySessionHelper;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;

public abstract class AbstractSQLCoreProvider extends AuthCoreProvider implements AuthSupportSudo {
    public final transient Logger logger = LogManager.getLogger();
    public long expireSeconds = HOURS.toSeconds(1);
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
    public String customQueryRolesByUserUUID;
    public String customUpdateAuthSQL;
    public String customUpdateServerIdSQL;
    // Prepared SQL queries
    public transient String queryByUUIDSQL;
    public transient String queryByUsernameSQL;
    public transient String queryByLoginSQL;
    public transient String queryPermissionsByUUIDSQL;
    public transient String queryRolesByUserUUID;

    public transient String updateAuthSQL;
    public transient String updateServerIDSQL;

    public abstract SQLSourceConfig getSQLConfig();

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
            if (user == null) {
                return null;
            }
            return createSession(user);
        } catch (ExpiredJwtException e) {
            throw new OAuthAccessTokenExpired();
        } catch (JwtException e) {
            return null;
        }
    }

    @Override
    public AuthManager.AuthReport refreshAccessToken(String refreshToken, AuthResponse.AuthContext context) {
        String[] parts = refreshToken.split("\\.");
        if (parts.length != 2) {
            return null;
        }
        String username = parts[0];
        String token = parts[1];
        var user = (SQLUser) getUserByUsername(username);
        if (user == null || user.password == null) {
            return null;
        }
        var realToken = LegacySessionHelper.makeRefreshTokenFromPassword(username, user.password, server.keyAgreementManager.legacySalt);
        if (!token.equals(realToken)) {
            return null;
        }
        var accessToken = LegacySessionHelper.makeAccessJwtTokenFromString(user, LocalDateTime.now(Clock.systemUTC()).plusSeconds(expireSeconds), server.keyAgreementManager.ecdsaPrivateKey);
        return new AuthManager.AuthReport(null, accessToken, refreshToken, SECONDS.toMillis(expireSeconds), createSession(user));
    }

    @Override
    public AuthManager.AuthReport authorize(String login, AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password, boolean minecraftAccess) throws IOException {
        SQLUser user = (SQLUser) getUserByLogin(login);
        if (user == null) {
            throw AuthException.userNotFound();
        }
        AuthPlainPassword plainPassword = (AuthPlainPassword) password;
        if (plainPassword == null) {
            throw AuthException.wrongPassword();
        }
        if (!passwordVerifier.check(user.password, plainPassword.password)) {
            throw AuthException.wrongPassword();
        }
        SQLUserSession session = createSession(user);
        var accessToken = LegacySessionHelper.makeAccessJwtTokenFromString(user, LocalDateTime.now(Clock.systemUTC()).plusSeconds(expireSeconds), server.keyAgreementManager.ecdsaPrivateKey);
        var refreshToken = user.username.concat(".").concat(LegacySessionHelper.makeRefreshTokenFromPassword(user.username, user.password, server.keyAgreementManager.legacySalt));
        if (minecraftAccess) {
            String minecraftAccessToken = SecurityHelper.randomStringToken();
            updateAuth(user, minecraftAccessToken);
            return AuthManager.AuthReport.ofOAuthWithMinecraft(minecraftAccessToken, accessToken, refreshToken, SECONDS.toMillis(expireSeconds), session);
        } else {
            return AuthManager.AuthReport.ofOAuth(accessToken, refreshToken, SECONDS.toMillis(expireSeconds), session);
        }
    }

    @Override
    public AuthManager.AuthReport sudo(User user, boolean shadow) throws IOException {
        SQLUser sqlUser = (SQLUser) user;
        SQLUserSession session = createSession(sqlUser);
        var accessToken = LegacySessionHelper.makeAccessJwtTokenFromString(sqlUser, LocalDateTime.now(Clock.systemUTC()).plusSeconds(expireSeconds), server.keyAgreementManager.ecdsaPrivateKey);
        var refreshToken = sqlUser.username.concat(".").concat(LegacySessionHelper.makeRefreshTokenFromPassword(sqlUser.username, sqlUser.password, server.keyAgreementManager.legacySalt));
        String minecraftAccessToken = SecurityHelper.randomStringToken();
        updateAuth(user, minecraftAccessToken);
        return AuthManager.AuthReport.ofOAuthWithMinecraft(minecraftAccessToken, accessToken, refreshToken, SECONDS.toMillis(expireSeconds), session);
    }

    @Override
    public User checkServer(Client client, String username, String serverID) throws IOException {
        SQLUser user = (SQLUser) getUserByUsername(username);
        if (user == null) {
            return null;
        }
        if (user.getUsername().equals(username) && user.getServerId().equals(serverID)) {
            return user;
        }
        return null;
    }

    @Override
    public boolean joinServer(Client client, String username, UUID uuid, String accessToken, String serverID) throws IOException {
        SQLUser user = (SQLUser) client.getUser();
        if (user == null) return false;
        return (uuid == null ? user.getUsername().equals(username) : user.getUUID().equals(uuid)) && user.getAccessToken().equals(accessToken) && updateServerID(user, serverID);
    }

    @Override
    public void init(LaunchServer server, AuthProviderPair pair) {
        super.init(server, pair);
        if (getSQLConfig() == null) logger.error("SQLHolder cannot be null");
        if (uuidColumn == null) logger.error("uuidColumn cannot be null");
        if (usernameColumn == null) logger.error("usernameColumn cannot be null");
        if (accessTokenColumn == null) logger.error("accessTokenColumn cannot be null");
        if (serverIDColumn == null) logger.error("serverIDColumn cannot be null");
        if (table == null) logger.error("table cannot be null");
        // Prepare SQL queries
        String userInfoCols = makeUserCols();
        queryByUUIDSQL = customQueryByUUIDSQL != null ? customQueryByUUIDSQL :
                "SELECT %s FROM %s WHERE %s=? LIMIT 1".formatted(userInfoCols, table, uuidColumn);
        queryByUsernameSQL = customQueryByUsernameSQL != null ? customQueryByUsernameSQL :
                "SELECT %s FROM %s WHERE %s=? LIMIT 1".formatted(userInfoCols, table, usernameColumn);
        queryByLoginSQL = customQueryByLoginSQL != null ? customQueryByLoginSQL : queryByUsernameSQL;




                
        updateAuthSQL = customUpdateAuthSQL != null ? customUpdateAuthSQL :
                "UPDATE %s SET %s=?, %s=NULL WHERE %s=?".formatted(table, accessTokenColumn, serverIDColumn, uuidColumn);
        updateServerIDSQL = customUpdateServerIdSQL != null ? customUpdateServerIdSQL :
                "UPDATE %s SET %s=? WHERE %s=?".formatted(table, serverIDColumn, uuidColumn);
        if (isEnabledPermissions()) {
            if(isEnabledRoles()) {
                queryPermissionsByUUIDSQL = customQueryPermissionsByUUIDSQL != null ? customQueryPermissionsByUUIDSQL :
                        "WITH RECURSIVE req AS (\n" +
                                "SELECT p."+permissionsPermissionColumn+" FROM "+permissionsTable+" p WHERE p."+permissionsUUIDColumn+" = ?\n" +
                                "UNION ALL\n" +
                                "SELECT p."+permissionsPermissionColumn+" FROM "+permissionsTable+" p\n" +
                                "INNER JOIN "+rolesTable+" r ON p."+permissionsUUIDColumn+" = r."+rolesUUIDColumn+"\n" +
                                "INNER JOIN req ON r."+rolesUUIDColumn+"=substring(req."+permissionsPermissionColumn+" from 6) or r.name=substring(req."+permissionsPermissionColumn+" from 6)\n" +
                                ") SELECT * FROM req";
                queryRolesByUserUUID = customQueryRolesByUserUUID != null ? customQueryRolesByUserUUID : "SELECT r." + rolesNameColumn + " FROM " + rolesTable + " r\n" +
                        "INNER JOIN " + permissionsTable + " pr ON r." + rolesUUIDColumn + "=substring(pr." + permissionsPermissionColumn + " from 6) or r." + rolesNameColumn + "=substring(pr." + permissionsPermissionColumn + " from 6)\n" +
                        "WHERE pr." + permissionsUUIDColumn + " = ?";
            } else {
                queryPermissionsByUUIDSQL = customQueryPermissionsByUUIDSQL != null ? customQueryPermissionsByUUIDSQL :
                        "SELECT (%s) FROM %s WHERE %s=?".formatted(permissionsPermissionColumn, permissionsTable, permissionsUUIDColumn);
            }
        }
    }

    protected String makeUserCols() {
        return "%s, %s, %s, %s, %s".formatted(uuidColumn, usernameColumn, accessTokenColumn, serverIDColumn, passwordColumn);
    }

    protected void updateAuth(User user, String accessToken) throws IOException {
        try (Connection c = getSQLConfig().getConnection()) {
            SQLUser SQLUser = (SQLUser) user;
            SQLUser.accessToken = accessToken;
            PreparedStatement s = c.prepareStatement(updateAuthSQL);
            s.setString(1, accessToken);
            s.setString(2, user.getUUID().toString());
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            s.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

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
    public void close() {
        getSQLConfig().close();
    }

    protected SQLUser constructUser(ResultSet set) throws SQLException {
        return set.next() ? new SQLUser(UUID.fromString(set.getString(uuidColumn)), set.getString(usernameColumn),
                set.getString(accessTokenColumn), set.getString(serverIDColumn), set.getString(passwordColumn)) : null;
    }

    public ClientPermissions requestPermissions (String uuid)  throws SQLException
    {
        return new ClientPermissions(isEnabledRoles() ? queryRolesNames(queryRolesByUserUUID,uuid) : new ArrayList<>(),
                isEnabledPermissions() ? queryPermissions(queryPermissionsByUUIDSQL,uuid) : new ArrayList<>());
    }

    private SQLUser queryUser(String sql, String value) throws SQLException {
        SQLUser user;
        try (Connection c = getSQLConfig().getConnection()) {
            PreparedStatement s = c.prepareStatement(sql);
            s.setString(1, value);
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            try (ResultSet set = s.executeQuery()) {
                user = constructUser(set);
            }
        }
        if(user != null) {
            user.permissions = requestPermissions(user.uuid.toString());
        }
        return user;
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
        }
    }

    protected SQLUserSession createSession(SQLUser user) {
        return new SQLUserSession(user);
    }

    public boolean isEnabledPermissions() {
        return permissionsPermissionColumn != null;
    }

    public boolean isEnabledRoles() {
        return rolesNameColumn != null;
    }

    private List<String> queryRolesNames(String sql, String value) throws SQLException {
        try (Connection c = getSQLConfig().getConnection()) {
            PreparedStatement s = c.prepareStatement(sql);
            s.setString(1, value);
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            ResultSet set = s.executeQuery();
            List<String> perms = new ArrayList<>();
            while (set.next())
                perms.add(set.getString(rolesNameColumn));
            return perms;
        }
    }

    public static class SQLUser implements User {
        protected final UUID uuid;
        protected final String username;
        protected String accessToken;
        protected String serverId;
        protected final String password;
        protected ClientPermissions permissions;

        public SQLUser(UUID uuid, String username, String accessToken, String serverId, String password) {
            this.uuid = uuid;
            this.username = username;
            this.accessToken = accessToken;
            this.serverId = serverId;
            this.password = password;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public UUID getUUID() {
            return uuid;
        }

        public String getServerId() {
            return serverId;
        }

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
        public String getMinecraftAccessToken() {
            return user.getAccessToken();
        }

        @Override
        public long getExpireIn() {
            return 0;
        }
    }
}
