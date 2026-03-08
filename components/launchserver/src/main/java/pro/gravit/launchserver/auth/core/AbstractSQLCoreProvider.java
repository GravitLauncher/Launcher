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
import java.sql.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.HOURS;

/**
 * Abstract SQL-backed {@link AuthCoreProvider}.
 *
 * <h3>Configuration fields (serialized)</h3>
 * <ul>
 *   <li>Core user columns – {@link #uuidColumn}, {@link #usernameColumn},
 *       {@link #accessTokenColumn}, {@link #serverIDColumn}, {@link #passwordColumn}
 *       – and {@link #table} are <b>required</b>.</li>
 *   <li>Permissions/roles columns are optional; their presence enables the
 *       respective query path automatically.</li>
 *   <li>Any {@code customQuery*} / {@code customUpdate*} field overrides the
 *       corresponding generated SQL when non-null.</li>
 * </ul>
 */
public abstract class AbstractSQLCoreProvider extends AuthCoreProvider implements AuthSupportSudo {

    // -------------------------------------------------------------------------
    // Shared logger – intentionally non-private so subclasses can reuse it
    // -------------------------------------------------------------------------
    protected final transient Logger logger = LogManager.getLogger(getClass());

    // -------------------------------------------------------------------------
    // Serialized configuration
    // -------------------------------------------------------------------------

    /** Session / access-token lifetime in seconds (default: 1 hour). */
    public long expireSeconds = HOURS.toSeconds(1);

    // Core user table
    public String table;
    public String uuidColumn;
    public String usernameColumn;
    public String accessTokenColumn;
    public String passwordColumn;
    public String serverIDColumn;

    // Optional permissions table (leave null to disable)
    public String permissionsTable;
    public String permissionsPermissionColumn;
    public String permissionsUUIDColumn;

    // Optional roles table (leave null to disable; requires permissionsTable)
    public String rolesTable;
    public String rolesNameColumn;
    public String rolesUUIDColumn;

    public PasswordVerifier passwordVerifier;

    // Custom SQL overrides – any field left null causes the default query to be used
    public String customQueryByUUIDSQL;
    public String customQueryByUsernameSQL;
    public String customQueryByLoginSQL;
    public String customQueryPermissionsByUUIDSQL;
    public String customQueryRolesByUserUUID;
    public String customUpdateAuthSQL;
    public String customUpdateServerIdSQL;

    // -------------------------------------------------------------------------
    // Prepared SQL (transient – rebuilt on every init)
    // -------------------------------------------------------------------------
    private transient String queryByUUIDSQL;
    private transient String queryByUsernameSQL;
    private transient String queryByLoginSQL;
    private transient String queryPermissionsByUUIDSQL;
    private transient String queryRolesByUserUUID;
    private transient String updateAuthSQL;
    private transient String updateServerIDSQL;

    // -------------------------------------------------------------------------
    // Abstract contract
    // -------------------------------------------------------------------------

    public abstract SQLSourceConfig getSQLConfig();

    // -------------------------------------------------------------------------
    // AuthCoreProvider – user lookup
    // -------------------------------------------------------------------------

    @Override
    public User getUserByUsername(String username) {
        return safeQueryUser(queryByUsernameSQL, username);
    }

    @Override
    public User getUserByUUID(UUID uuid) {
        return safeQueryUser(queryByUUIDSQL, uuid.toString());
    }

    @Override
    public User getUserByLogin(String login) {
        return safeQueryUser(queryByLoginSQL, login);
    }

    // -------------------------------------------------------------------------
    // AuthCoreProvider – session management
    // -------------------------------------------------------------------------

    @Override
    public UserSession getUserSessionByOAuthAccessToken(String accessToken) throws OAuthAccessTokenExpired {
        try {
            var info = LegacySessionHelper.getJwtInfoFromAccessToken(
                    accessToken, server.keyAgreementManager.ecdsaPublicKey);
            SQLUser user = (SQLUser) getUserByUUID(info.uuid());
            return user == null ? null : createSession(user);
        } catch (ExpiredJwtException e) {
            throw new OAuthAccessTokenExpired();
        } catch (JwtException e) {
            return null;
        }
    }

    @Override
    public AuthManager.AuthReport refreshAccessToken(String refreshToken, AuthResponse.AuthContext context) {
        String[] parts = refreshToken.split("\\.", 2);
        if (parts.length != 2) return null;

        SQLUser user = (SQLUser) getUserByUsername(parts[0]);
        if (user == null || user.password == null) return null;

        String expected = LegacySessionHelper.makeRefreshTokenFromPassword(
                parts[0], user.password, server.keyAgreementManager.legacySalt);
        if (!expected.equals(parts[1])) return null;

        var accessToken = makeAccessToken(user);
        return new AuthManager.AuthReport(null, accessToken, refreshToken, expireSeconds, createSession(user));
    }

    @Override
    public AuthManager.AuthReport authorize(
            String login,
            AuthResponse.AuthContext context,
            AuthRequest.AuthPasswordInterface password,
            boolean minecraftAccess) throws IOException {

        SQLUser user = (SQLUser) getUserByLogin(login);
        if (user == null) throw AuthException.userNotFound();

        if (!(password instanceof AuthPlainPassword plain)) throw AuthException.wrongPassword();
        if (!passwordVerifier.check(user.password, plain.password)) throw AuthException.wrongPassword();

        return buildAuthReport(user, minecraftAccess);
    }

    @Override
    public AuthManager.AuthReport sudo(User user, boolean shadow) throws IOException {
        SQLUser sqlUser = (SQLUser) user;
        return buildAuthReport(sqlUser, true);
    }

    // -------------------------------------------------------------------------
    // AuthCoreProvider – server join / check
    // -------------------------------------------------------------------------

    @Override
    public User checkServer(Client client, String username, String serverID) {
        SQLUser user = (SQLUser) getUserByUsername(username);
        if (user == null) return null;
        return user.getUsername().equals(username) && user.getServerId().equals(serverID) ? user : null;
    }

    @Override
    public boolean joinServer(Client client, String username, UUID uuid, String accessToken, String serverID)
            throws IOException {
        SQLUser user = (SQLUser) client.getUser();
        if (user == null) return false;
        boolean identityMatch = uuid == null
                ? user.getUsername().equals(username)
                : user.getUUID().equals(uuid);
        return identityMatch && user.getAccessToken().equals(accessToken) && updateServerID(user, serverID);
    }

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    @Override
    public void init(LaunchServer server, AuthProviderPair pair) {
        super.init(server, pair);
        validateRequiredConfig();
        buildPreparedQueries();
    }

    private void validateRequiredConfig() {
        if (getSQLConfig() == null)    logger.error("sqlConfig (holder) cannot be null");
        if (table == null)             logger.error("table cannot be null");
        if (uuidColumn == null)        logger.error("uuidColumn cannot be null");
        if (usernameColumn == null)    logger.error("usernameColumn cannot be null");
        if (accessTokenColumn == null) logger.error("accessTokenColumn cannot be null");
        if (serverIDColumn == null)    logger.error("serverIDColumn cannot be null");
    }

    /**
     * Computes all transient SQL strings.  Subclasses may call {@code super}
     * and then set their own additional queries.
     */
    protected void buildPreparedQueries() {
        String cols = makeUserCols();

        queryByUUIDSQL     = resolve(customQueryByUUIDSQL,
                "SELECT %s FROM %s WHERE %s=? LIMIT 1".formatted(cols, table, uuidColumn));
        queryByUsernameSQL = resolve(customQueryByUsernameSQL,
                "SELECT %s FROM %s WHERE %s=? LIMIT 1".formatted(cols, table, usernameColumn));
        queryByLoginSQL    = resolve(customQueryByLoginSQL, queryByUsernameSQL);

        updateAuthSQL    = resolve(customUpdateAuthSQL,
                "UPDATE %s SET %s=?, %s=NULL WHERE %s=?".formatted(table, accessTokenColumn, serverIDColumn, uuidColumn));
        updateServerIDSQL = resolve(customUpdateServerIdSQL,
                "UPDATE %s SET %s=? WHERE %s=?".formatted(table, serverIDColumn, uuidColumn));

        buildPermissionQueries();
    }

    private void buildPermissionQueries() {
        if (!isPermissionsEnabled()) return;

        if (isRolesEnabled()) {
            // Recursive CTE that resolves role-inherited permissions
            queryPermissionsByUUIDSQL = resolve(customQueryPermissionsByUUIDSQL, """
                    WITH RECURSIVE req AS (
                      SELECT p.%s FROM %s p WHERE p.%s = ?
                      UNION ALL
                      SELECT p.%s FROM %s p
                      INNER JOIN %s r ON p.%s = r.%s
                      INNER JOIN req ON r.%s = substring(req.%s FROM 6)
                                     OR r.name = substring(req.%s FROM 6)
                    ) SELECT * FROM req""".formatted(
                    permissionsPermissionColumn, permissionsTable, permissionsUUIDColumn,
                    permissionsPermissionColumn, permissionsTable,
                    rolesTable, permissionsUUIDColumn, rolesUUIDColumn,
                    rolesUUIDColumn, permissionsPermissionColumn, permissionsPermissionColumn));

            queryRolesByUserUUID = resolve(customQueryRolesByUserUUID, """
                    SELECT r.%s FROM %s r
                    INNER JOIN %s pr ON r.%s = substring(pr.%s FROM 6)
                                     OR r.%s = substring(pr.%s FROM 6)
                    WHERE pr.%s = ?""".formatted(
                    rolesNameColumn, rolesTable,
                    permissionsTable, rolesUUIDColumn, permissionsPermissionColumn,
                    rolesNameColumn, permissionsPermissionColumn,
                    permissionsUUIDColumn));
        } else {
            queryPermissionsByUUIDSQL = resolve(customQueryPermissionsByUUIDSQL,
                    "SELECT %s FROM %s WHERE %s=?".formatted(
                            permissionsPermissionColumn, permissionsTable, permissionsUUIDColumn));
        }
    }

    @Override
    public void close() {
        getSQLConfig().close();
    }

    // -------------------------------------------------------------------------
    // SQL helpers – intended for use by subclasses
    // -------------------------------------------------------------------------

    /**
     * Returns a comma-separated list of all user columns selected in user queries.
     * Subclasses should override to append additional columns, e.g.:
     * <pre>{@code
     * protected String makeUserCols() {
     *     return super.makeUserCols() + ", " + hardwareIdColumn;
     * }
     * }</pre>
     */
    protected String makeUserCols() {
        return "%s, %s, %s, %s, %s".formatted(
                uuidColumn, usernameColumn, accessTokenColumn, serverIDColumn, passwordColumn);
    }

    /**
     * Constructs a {@link SQLUser} from the current row of {@code set}.
     * Returns {@code null} when the result set is empty.
     * Subclasses override this to populate additional fields.
     */
    protected SQLUser constructUser(ResultSet set) throws SQLException {
        if (!set.next()) return null;
        return new SQLUser(
                UUID.fromString(set.getString(uuidColumn)),
                set.getString(usernameColumn),
                set.getString(accessTokenColumn),
                set.getString(serverIDColumn),
                set.getString(passwordColumn));
    }

    protected SQLUserSession createSession(SQLUser user) {
        return new SQLUserSession(user);
    }

    /**
     * Executes a single-parameter query and maps the result to a {@link SQLUser},
     * enriching it with permissions.  Returns {@code null} on SQL errors.
     */
    protected final SQLUser safeQueryUser(String sql, String param) {
        try {
            return queryUser(sql, param);
        } catch (Exception e) {
            logger.error("SQL error while querying user (param={})", param, e);
            return null;
        }
    }

    private SQLUser queryUser(String sql, String param) throws SQLException {
        try (Connection c = getSQLConfig().getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, param);
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            try (ResultSet rs = s.executeQuery()) {
                SQLUser user = constructUser(rs);
                if (user != null) {
                    user.permissions = loadPermissions(user.uuid.toString());
                }
                return user;
            }
        }
    }

    protected void updateAuth(User user, String accessToken) throws IOException {
        SQLUser sqlUser = (SQLUser) user;
        sqlUser.accessToken = accessToken;
        executeSingleUpdate(updateAuthSQL, accessToken, user.getUUID().toString());
    }

    protected boolean updateServerID(User user, String serverID) throws IOException {
        SQLUser sqlUser = (SQLUser) user;
        sqlUser.serverId = serverID;
        return executeSingleUpdate(updateServerIDSQL, serverID, user.getUUID().toString()) > 0;
    }

    /**
     * Runs a two-parameter UPDATE and returns the affected-row count.
     * Wraps {@link SQLException} as {@link IOException}.
     */
    protected int executeSingleUpdate(String sql, String p1, String p2) throws IOException {
        try (Connection c = getSQLConfig().getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, p1);
            s.setString(2, p2);
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            return s.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    // -------------------------------------------------------------------------
    // Permissions & roles
    // -------------------------------------------------------------------------

    public boolean isPermissionsEnabled() {
        return permissionsPermissionColumn != null;
    }

    public boolean isRolesEnabled() {
        return rolesNameColumn != null;
    }

    public ClientPermissions loadPermissions(String uuid) throws SQLException {
        List<String> roles = isRolesEnabled()
                ? queryStringColumn(queryRolesByUserUUID, uuid, rolesNameColumn)
                : List.of();
        List<String> perms = isPermissionsEnabled()
                ? queryStringColumn(queryPermissionsByUUIDSQL, uuid, permissionsPermissionColumn)
                : List.of();
        return new ClientPermissions(roles, perms);
    }

    private List<String> queryStringColumn(String sql, String param, String column) throws SQLException {
        try (Connection c = getSQLConfig().getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, param);
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            try (ResultSet rs = s.executeQuery()) {
                List<String> result = new ArrayList<>();
                while (rs.next()) result.add(rs.getString(column));
                return result;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Returns {@code override} when non-null, otherwise {@code fallback}. */
    private static String resolve(String override, String fallback) {
        return override != null ? override : fallback;
    }

    private String makeAccessToken(SQLUser user) {
        return LegacySessionHelper.makeAccessJwtTokenFromString(
                user,
                LocalDateTime.now(Clock.systemUTC()).plusSeconds(expireSeconds),
                server.keyAgreementManager.ecdsaPrivateKey);
    }

    private String makeRefreshToken(SQLUser user) {
        return user.username + "." + LegacySessionHelper.makeRefreshTokenFromPassword(
                user.username, user.password, server.keyAgreementManager.legacySalt);
    }

    private AuthManager.AuthReport buildAuthReport(SQLUser user, boolean minecraftAccess) throws IOException {
        SQLUserSession session  = createSession(user);
        String accessToken      = makeAccessToken(user);
        String refreshToken     = makeRefreshToken(user);
        if (minecraftAccess) {
            String mcToken = SecurityHelper.randomStringToken();
            updateAuth(user, mcToken);
            return AuthManager.AuthReport.ofOAuthWithMinecraft(mcToken, accessToken, refreshToken, expireSeconds, session);
        }
        return AuthManager.AuthReport.ofOAuth(accessToken, refreshToken, expireSeconds, session);
    }

    // =========================================================================
    // Inner types
    // =========================================================================

    public static class SQLUser implements User {
        protected final UUID uuid;
        protected final String username;
        protected String accessToken;
        protected String serverId;
        /** Stored hashed password (never exposed via the {@link User} API). */
        protected final String password;
        protected ClientPermissions permissions;

        public SQLUser(UUID uuid, String username, String accessToken, String serverId, String password) {
            this.uuid        = uuid;
            this.username    = username;
            this.accessToken = accessToken;
            this.serverId    = serverId;
            this.password    = password;
        }

        @Override public String getUsername()             { return username; }
        @Override public UUID getUUID()                   { return uuid; }
        @Override public ClientPermissions getPermissions(){ return permissions; }

        public String getServerId()    { return serverId; }
        public String getAccessToken() { return accessToken; }

        @Override
        public String toString() {
            return "SQLUser{uuid=%s, username='%s', permissions=%s}".formatted(uuid, username, permissions);
        }
    }

    public static class SQLUserSession implements UserSession {
        private final SQLUser user;
        private final String id;

        public SQLUserSession(SQLUser user) {
            this.user = user;
            this.id   = user.username;
        }

        @Override public String getID()                   { return id; }
        @Override public User getUser()                   { return user; }
        @Override public String getMinecraftAccessToken() { return user.getAccessToken(); }
        @Override public long getExpireIn()               { return 0; }
    }
}