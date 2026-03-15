package pro.gravit.launchserver.auth.core;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.base.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.base.request.auth.AuthRequest;
import pro.gravit.launcher.base.request.auth.details.AuthPasswordDetails;
import pro.gravit.launcher.base.request.auth.details.AuthTotpDetails;
import pro.gravit.launcher.base.request.auth.password.Auth2FAPassword;
import pro.gravit.launcher.base.request.auth.password.AuthMultiPassword;
import pro.gravit.launcher.base.request.auth.password.AuthPlainPassword;
import pro.gravit.launcher.base.request.auth.password.AuthTOTPPassword;
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
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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
 *
 * <h3>Adding an optional user-table column</h3>
 * Override {@link #registerOptionalColumns()} and call
 * {@link #registerColumnFeature(ColumnFeature)} for each optional column.
 * The feature is automatically included in SELECT lists and ResultSet mapping
 * only when its column name is non-null — no other methods need to be touched.
 *
 * <pre>{@code
 * // Example: totpSecret optional column in a subclass
 * public String totpSecretColumn;   // null = feature disabled
 *
 * @Override
 * protected void registerOptionalColumns() {
 *     super.registerOptionalColumns();
 *     registerColumnFeature(ColumnFeature.of(
 *         totpSecretColumn,
 *         (user, rs) -> ((MyUser) user).totpSecret = rs.getString(totpSecretColumn)
 *     ));
 * }
 * }</pre>
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

    /**
     * Optional TOTP secret column in the user table.
     * Set to a non-null column name to enable TOTP support; leave {@code null} to disable.
     * When disabled the column is completely absent from all generated SQL.
     */
    public String totpSecretColumn;

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
    // Transient state – rebuilt on every init()
    // -------------------------------------------------------------------------

    /**
     * Registry of all <em>enabled</em> optional user-table columns.
     * Populated by {@link #registerOptionalColumns()}; disabled features are
     * removed before any SQL is generated.
     */
    private transient List<ColumnFeature<?, ?>> optionalColumns;

    private transient String queryByUUIDSQL;
    private transient String queryByUsernameSQL;
    private transient String queryByLoginSQL;
    private transient String queryPermissionsByUUIDSQL;
    private transient String queryRolesByUserUUID;
    private transient String updateAuthSQL;
    private transient String updateServerIDSQL;
    protected transient final TimeProvider timeProvider = new SystemTimeProvider();
    protected transient final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    protected transient final CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

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

    @Override
    public List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> getDetails(Client client) {
        if(totpSecretColumn == null) {
            return List.of(new AuthPasswordDetails());
        } else {
            return List.of(new AuthPasswordDetails(), new AuthTotpDetails("HMAC", 6));
        }
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
        String plainPassword;
        String totpCode;
        if (password instanceof AuthPlainPassword plain) {
            plainPassword = plain.password;
            totpCode = null;
        } else if(password instanceof Auth2FAPassword twoFa) {
            if(twoFa.firstPassword instanceof AuthPlainPassword plain) {
                plainPassword = plain.password;
            } else {
                throw AuthException.wrongPassword();
            }
            if(twoFa.secondPassword instanceof AuthTOTPPassword totpPassword) {
                totpCode = totpPassword.totp;
            } else {
                throw AuthException.wrongPassword();
            }
        } else if(password instanceof AuthMultiPassword multi) {
            if(multi.list == null || multi.list.isEmpty()) {
                throw AuthException.wrongPassword();
            }
            if(multi.list.get(0) instanceof AuthPlainPassword plain) {
                plainPassword = plain.password;
            } else {
                throw AuthException.wrongPassword();
            }
            if(multi.list.get(1) instanceof AuthTOTPPassword totpPassword) {
                totpCode = totpPassword.totp;
            } else {
                throw AuthException.wrongPassword();
            }
        } else {
            throw AuthException.wrongPassword();
        }
        if (!passwordVerifier.check(user.password, plainPassword)) throw AuthException.wrongPassword();
        if (totpSecretColumn != null && user.totpSecret != null) {
            if(totpCode == null) {
                throw AuthException.need2FA();
            }
            verifyTotpCode(user, totpCode);
        }


        return buildAuthReport(user, minecraftAccess);
    }

    protected void verifyTotpCode(SQLUser user, String totpCode) throws AuthException {
        if(!verifier.isValidCode(user.totpSecret, totpCode)) {
            throw AuthException.wrongPassword();
        }
    }

    @Override
    public AuthManager.AuthReport sudo(User user, boolean shadow) throws IOException {
        return buildAuthReport((SQLUser) user, true);
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

        // Phase 1: collect optional column declarations from the full class hierarchy,
        // then immediately discard disabled ones so the rest of the code never sees them.
        optionalColumns = new ArrayList<>();
        registerOptionalColumns();
        optionalColumns.removeIf(f -> !f.isEnabled());

        // Phase 2: build SQL now that the final column list is known.
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

    // -------------------------------------------------------------------------
    // Optional column registry hook
    // -------------------------------------------------------------------------

    /**
     * Override to declare optional user-table columns.
     * Always call {@code super.registerOptionalColumns()} so the full hierarchy participates.
     *
     * <p>Registering a feature with a {@code null} column name is safe — it is
     * silently ignored before any SQL is generated.
     *
     * <p>See the class-level Javadoc for a usage example.
     */
    protected void registerOptionalColumns() {

        registerColumnFeature(ColumnFeature.of(
                totpSecretColumn,
                (user, rs) -> {
                    try {
                        user.totpSecret = rs.getString(totpSecretColumn);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        ));
    }

    /**
     * Registers one optional column.  Only valid to call from
     * {@link #registerOptionalColumns()}.
     */
    protected final void registerColumnFeature(ColumnFeature<?, ?> feature) {
        optionalColumns.add(feature);
    }

    // -------------------------------------------------------------------------
    // SQL construction
    // -------------------------------------------------------------------------

    /**
     * Builds all transient SQL strings.  Called once per {@link #init} after the
     * optional-column registry is finalised.
     *
     * <p>Subclasses that need additional SQL should override this and call
     * {@code super.buildPreparedQueries()} first.
     */
    protected void buildPreparedQueries() {
        String cols = makeUserCols();

        queryByUUIDSQL     = resolve(customQueryByUUIDSQL,
                "SELECT %s FROM %s WHERE %s=? LIMIT 1".formatted(cols, table, uuidColumn));
        queryByUsernameSQL = resolve(customQueryByUsernameSQL,
                "SELECT %s FROM %s WHERE %s=? LIMIT 1".formatted(cols, table, usernameColumn));
        queryByLoginSQL    = resolve(customQueryByLoginSQL, queryByUsernameSQL);

        updateAuthSQL     = resolve(customUpdateAuthSQL,
                "UPDATE %s SET %s=?, %s=NULL WHERE %s=?".formatted(
                        table, accessTokenColumn, serverIDColumn, uuidColumn));
        updateServerIDSQL = resolve(customUpdateServerIdSQL,
                "UPDATE %s SET %s=? WHERE %s=?".formatted(table, serverIDColumn, uuidColumn));

        buildPermissionQueries();
    }

    /**
     * Returns the comma-separated column list used in SELECT queries.
     *
     * <p>The base implementation covers all required columns plus every enabled
     * optional column registered via {@link #registerOptionalColumns()}.
     *
     * <p>Subclasses that add extra <em>required</em> (always-present) columns should
     * override this and append to {@code super.makeUserCols()}.  Truly optional columns
     * belong in {@link #registerOptionalColumns()} instead — they are appended here
     * automatically.
     */
    protected String makeUserCols() {
        String required = "%s, %s, %s, %s, %s".formatted(
                uuidColumn, usernameColumn, accessTokenColumn, serverIDColumn, passwordColumn);

        if (optionalColumns.isEmpty()) return required;

        String optional = optionalColumns.stream()
                .map(f -> f.columnName)
                .collect(Collectors.joining(", "));
        return required + ", " + optional;
    }

    /**
     * Constructs a {@link SQLUser} from the <em>current</em> ResultSet row.
     * Assumes {@link ResultSet#next()} has already returned {@code true}.
     *
     * <p>Required columns are always read.  Every enabled optional column is then
     * applied via its {@link ColumnFeature} reader — no per-feature null checks needed.
     *
     * <p>Subclasses that add their own <em>required</em> columns should override this,
     * call {@code super.constructUserFromRow(set)}, cast the result to their SQLUser
     * subtype, and populate the extra fields.  Optional columns should use
     * {@link #registerOptionalColumns()} instead.
     */
    protected SQLUser constructUserFromRow(ResultSet set) throws SQLException {
        SQLUser user = new SQLUser(
                UUID.fromString(set.getString(uuidColumn)),
                set.getString(usernameColumn),
                set.getString(accessTokenColumn),
                set.getString(serverIDColumn),
                set.getString(passwordColumn));
        applyOptionalColumns(user, set);
        return user;
    }

    /**
     * Advances {@code set} by one row and delegates to {@link #constructUserFromRow},
     * or returns {@code null} if the result set is empty.
     */
    protected SQLUser constructUser(ResultSet set) throws SQLException {
        return set.next() ? constructUserFromRow(set) : null;
    }

    /**
     * Applies all registered optional columns to {@code user} from the current row of {@code set}.
     *
     * <p>Subclasses that override {@link #constructUserFromRow} to supply their own
     * required columns should call this at the end of their override so optional columns
     * are still applied:
     *
     * <pre>{@code
     * @Override
     * protected SQLUser constructUserFromRow(ResultSet set) throws SQLException {
     *     MyUser user = new MyUser(..., set.getLong(myRequiredColumn));
     *     applyOptionalColumns(user, set);
     *     return user;
     * }
     * }</pre>
     */
    protected final void applyOptionalColumns(SQLUser user, ResultSet set) throws SQLException {
        for (ColumnFeature<?, ?> feature : optionalColumns) {
            feature.read(user, set);
        }
    }

    protected SQLUserSession createSession(SQLUser user) {
        return new SQLUserSession(user);
    }

    // -------------------------------------------------------------------------
    // SQL helpers – intended for use by subclasses
    // -------------------------------------------------------------------------

    /**
     * Executes a single-parameter SELECT and maps the result to a {@link SQLUser}
     * (with permissions).  Returns {@code null} on SQL errors.
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

    @Override
    public void close() {
        getSQLConfig().close();
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

    private void buildPermissionQueries() {
        if (!isPermissionsEnabled()) return;

        if (isRolesEnabled()) {
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

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String resolve(String override, String fallback) {
        return override != null ? override : fallback;
    }

    private String makeAccessToken(SQLUser user) {
        return LegacySessionHelper.makeAccessJwtTokenFromString(
                user,
                LocalDateTime.now().plusSeconds(expireSeconds),
                server.keyAgreementManager.ecdsaPrivateKey);
    }

    private String makeRefreshToken(SQLUser user) {
        return user.username + "." + LegacySessionHelper.makeRefreshTokenFromPassword(
                user.username, user.password, server.keyAgreementManager.legacySalt);
    }

    private AuthManager.AuthReport buildAuthReport(SQLUser user, boolean minecraftAccess) throws IOException {
        SQLUserSession session = createSession(user);
        String accessToken    = makeAccessToken(user);
        String refreshToken   = makeRefreshToken(user);
        if (minecraftAccess) {
            String mcToken = SecurityHelper.randomStringToken();
            updateAuth(user, mcToken);
            return AuthManager.AuthReport.ofOAuthWithMinecraft(
                    mcToken, accessToken, refreshToken, expireSeconds, session);
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
        /** Stored hashed password — never exposed through the {@link User} API. */
        protected final String password;
        protected ClientPermissions permissions;

        /**
         * TOTP shared secret for this user.
         * {@code null} when the {@code totpSecretColumn} feature is disabled.
         */
        public String totpSecret;

        public SQLUser(UUID uuid, String username, String accessToken, String serverId, String password) {
            this.uuid        = uuid;
            this.username    = username;
            this.accessToken = accessToken;
            this.serverId    = serverId;
            this.password    = password;
        }

        @Override public String getUsername()              { return username; }
        @Override public UUID getUUID()                    { return uuid; }
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

    /**
     * Describes a single optional column in the user table.
     *
     * <p>A {@code ColumnFeature} is <b>enabled</b> when its {@link #columnName} is non-null.
     * When disabled, it is silently excluded from every SQL fragment — SELECT column list,
     * UPDATE statements, and ResultSet mapping — with no branching required at the call site.
     *
     * <p>Instances are registered via
     * {@link AbstractSQLCoreProvider#registerColumnFeature(ColumnFeature)} inside
     * {@link AbstractSQLCoreProvider#registerOptionalColumns()}.
     *
     * @param <U> the concrete {@link SQLUser} subtype this feature reads into
     * @param <T> the Java type of the value held in the column
     */
    public static final class ColumnFeature<U extends SQLUser, T> {

        /**
         * Name of the database column.  {@code null} means the feature is disabled —
         * it will be excluded from all SQL and ResultSet access.
         */
        public final String columnName;

        /**
         * Reads this column from the current row of {@code rs} and stores it on {@code user}.
         * Never called when {@link #isEnabled()} returns {@code false}.
         */
        private final BiConsumer<U, ResultSet> reader;

        // -------------------------------------------------------------------------
        // Factory methods
        // -------------------------------------------------------------------------

        /**
         * Creates an enabled feature backed by a custom reader lambda.
         *
         * <pre>{@code
         * ColumnFeature.of(totpColumn, (user, rs) -> user.totpSecret = rs.getString(totpColumn))
         * }</pre>
         */
        public static <U extends SQLUser, T> ColumnFeature<U, T> of(
                String columnName,
                BiConsumer<U, ResultSet> reader) {
            return new ColumnFeature<>(columnName, reader);
        }

        /**
         * Convenience factory for columns whose value is a {@code String} mapped via
         * {@link ResultSet#getString(String)} and stored through a setter.
         *
         * <pre>{@code
         * ColumnFeature.ofString(totpColumn, rs -> rs.getString(totpColumn), (user, val) -> user.totpSecret = val)
         * }</pre>
         */
        public static <U extends SQLUser> ColumnFeature<U, String> ofString(
                String columnName,
                Function<ResultSet, String> extractor,
                BiConsumer<U, String> setter) {
            return new ColumnFeature<>(columnName,
                    (user, rs) -> {
                        try {
                            setter.accept(user, extractor.apply(rs));
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to read column '" + columnName + "'", e);
                        }
                    });
        }

        // -------------------------------------------------------------------------
        // Core API
        // -------------------------------------------------------------------------

        /** Returns {@code true} when {@link #columnName} is non-null (feature is active). */
        public boolean isEnabled() {
            return columnName != null;
        }

        /**
         * Reads this column from {@code rs} into {@code user}.
         * Must only be called when {@link #isEnabled()} is {@code true}.
         *
         * @throws SQLException propagated from {@link ResultSet} access
         */
        @SuppressWarnings("unchecked")
        public void read(SQLUser user, ResultSet rs) throws SQLException {
            try {
                reader.accept((U) user, rs);
            } catch (RuntimeException e) {
                if (e.getCause() instanceof SQLException sql) throw sql;
                throw e;
            }
        }

        // -------------------------------------------------------------------------
        // Private constructor
        // -------------------------------------------------------------------------

        private ColumnFeature(String columnName, BiConsumer<U, ResultSet> reader) {
            this.columnName = columnName;
            this.reader     = reader;
        }
    }
}