package pro.gravit.launchserver.auth.core;

import pro.gravit.launcher.base.request.secure.HardwareReportRequest;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.HikariSQLSourceConfig;
import pro.gravit.launchserver.auth.MySQLSourceConfig;
import pro.gravit.launchserver.auth.SQLSourceConfig;
import pro.gravit.launchserver.auth.core.interfaces.UserHardware;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportExtendedCheckServer;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportHardware;
import pro.gravit.launchserver.auth.core.interfaces.session.UserSessionSupportHardware;
import pro.gravit.launchserver.socket.Client;

import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * SQL-backed auth provider with hardware-ID tracking and optional TOTP support.
 *
 * <h3>Required configuration fields (in addition to base class)</h3>
 * <ul>
 *   <li>{@link #hardwareIdColumn} – FK column in the user table pointing to the hwids table</li>
 * </ul>
 *
 * <h3>Optional configuration fields</h3>
 * <ul>
 *   <li>{@link #totpSecretColumn} – when set, the TOTP secret is read from this column and
 *       stored on {@link SQLUser#totpSecret}.  When {@code null} the column is completely
 *       absent from every SELECT query and ResultSet mapping.</li>
 *   <li>{@link #tableHWID} – defaults to {@code "hwids"}</li>
 *   <li>{@link #tableHWIDLog} – defaults to {@code "hwidLog"}</li>
 *   <li>{@link #criticalCompareLevel} – fuzzy-match threshold, defaults to {@code 1.0}</li>
 * </ul>
 */
public class SQLCoreProvider extends AbstractSQLCoreProvider
        implements AuthSupportHardware, AuthSupportExtendedCheckServer {

    // -------------------------------------------------------------------------
    // Serialized configuration
    // -------------------------------------------------------------------------

    /** Hikari connection-pool config (required). */
    public HikariSQLSourceConfig holder;

    /**
     * Column in the user table that holds the hardware-ID foreign key (required).
     * This is a <em>required</em> column for this provider — it is always included
     * in SELECT queries via {@link #makeUserCols()}, not via the optional registry.
     */
    public String hardwareIdColumn;

    /** Hardware-info table name. */
    public String tableHWID    = "hwids";

    /** Hardware-change audit-log table name. */
    public String tableHWIDLog = "hwidLog";

    /**
     * Minimum similarity level for a hardware record to be considered a match
     * in {@link #getHardwareInfoByData}.
     */
    public double criticalCompareLevel = 1.0;

    // Custom SQL overrides for hardware queries (null → use generated defaults)
    public String customFindHardwareByPublicKey;
    public String customFindHardwareByData;
    public String customFindHardwareById;
    public String customCreateHardware;
    public String customCreateHWIDLog;
    public String customUpdateHardwarePublicKey;
    public String customUsersByHwidId;

    // -------------------------------------------------------------------------
    // Transient prepared SQL
    // -------------------------------------------------------------------------

    private transient String sqlFindHardwareByPublicKey;
    private transient String sqlFindHardwareByData;
    private transient String sqlFindHardwareByDataFiltered;
    private transient String sqlFindHardwareById;
    private transient String sqlCreateHardware;
    private transient String sqlCreateHWIDLog;
    private transient String sqlUpdateHardwarePublicKey;
    private transient String sqlUpdateHardwareBanned;
    private transient String sqlUpdateUserHwidId;
    private transient String sqlUsersByHwidId;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public SQLSourceConfig getSQLConfig() {
        return holder;
    }

    @Override
    public void init(LaunchServer server, AuthProviderPair pair) {
        holder.init();
        super.init(server, pair); // triggers registerOptionalColumns() then buildPreparedQueries()
    }

    // -------------------------------------------------------------------------
    // SQL construction
    // -------------------------------------------------------------------------

    /**
     * {@code hardwareIdColumn} is a <em>required</em> column for this provider, so it
     * is appended here rather than through the optional registry.  Optional columns
     * (e.g. {@link #totpSecretColumn}) are appended by {@link AbstractSQLCoreProvider}
     * automatically after the required set.
     */
    @Override
    protected String makeUserCols() {
        return super.makeUserCols() + ", " + hardwareIdColumn;
    }

    @Override
    protected void buildPreparedQueries() {
        super.buildPreparedQueries();

        String hwCols = hardwareColumns();

        sqlFindHardwareByPublicKey = resolve(customFindHardwareByPublicKey,
                "SELECT %s FROM %s WHERE publicKey = ?".formatted(hwCols, tableHWID));

        sqlFindHardwareById = resolve(customFindHardwareById,
                "SELECT %s FROM %s WHERE id = ?".formatted(hwCols, tableHWID));

        sqlFindHardwareByData = resolve(customFindHardwareByData,
                "SELECT %s FROM %s".formatted(hwCols, tableHWID));

        sqlFindHardwareByDataFiltered =
                "SELECT %s FROM %s WHERE hwDiskId = ? OR baseboardSerialNumber = ?".formatted(hwCols, tableHWID);

        sqlCreateHardware = resolve(customCreateHardware,
                ("INSERT INTO %s " +
                        "(publicKey, hwDiskId, baseboardSerialNumber, displayId, bitness, " +
                        " totalMemory, logicalProcessors, physicalProcessors, processorMaxFreq, " +
                        " graphicCard, battery, banned) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0')").formatted(tableHWID));

        sqlCreateHWIDLog = resolve(customCreateHWIDLog,
                "INSERT INTO %s (hwidId, newPublicKey) VALUES (?, ?)".formatted(tableHWIDLog));

        sqlUpdateHardwarePublicKey = resolve(customUpdateHardwarePublicKey,
                "UPDATE %s SET publicKey = ? WHERE id = ?".formatted(tableHWID));

        sqlUpdateHardwareBanned = "UPDATE %s SET banned = ? WHERE id = ?".formatted(tableHWID);
        sqlUpdateUserHwidId     = "UPDATE %s SET %s = ? WHERE %s = ?".formatted(table, hardwareIdColumn, uuidColumn);

        sqlUsersByHwidId = resolve(customUsersByHwidId,
                "SELECT %s FROM %s WHERE %s = ?".formatted(makeUserCols(), table, hardwareIdColumn));
    }

    // -------------------------------------------------------------------------
    // User construction
    // -------------------------------------------------------------------------

    /**
     * Reads all required columns plus {@link #hardwareIdColumn}, then lets the
     * base class apply every registered optional column (including {@link #totpSecretColumn}
     * when enabled) via the {@link ColumnFeature} registry.
     */
    @Override
    protected SQLUser constructUserFromRow(ResultSet set) throws SQLException {
        // Build a SQLUser (this subtype) with the required columns ...
        SQLUser user = new SQLUser(
                UUID.fromString(set.getString(uuidColumn)),
                set.getString(usernameColumn),
                set.getString(accessTokenColumn),
                set.getString(serverIDColumn),
                set.getString(passwordColumn),
                set.getLong(hardwareIdColumn));

        // ... then let the base class apply all registered optional columns.
        applyOptionalColumns(user, set);
        return user;
    }

    @Override
    public void close() {
        super.close(); // delegates to getSQLConfig().close() == holder.close()
    }

    // -------------------------------------------------------------------------
    // AuthSupportHardware
    // -------------------------------------------------------------------------

    @Override
    public UserHardware getHardwareInfoByPublicKey(byte[] publicKey) {
        try (Connection c = holder.getConnection();
             PreparedStatement s = c.prepareStatement(sqlFindHardwareByPublicKey)) {
            s.setBytes(1, publicKey);
            try (ResultSet rs = s.executeQuery()) {
                return rs.next() ? mapHardware(rs) : null;
            }
        } catch (SQLException e) {
            logger.error("SQL error in getHardwareInfoByPublicKey", e);
            return null;
        }
    }

    @Override
    public UserHardware getHardwareInfoByData(HardwareReportRequest.HardwareInfo info) {
        try (Connection c = holder.getConnection()) {
            SQLUserHardware result = findHardwareByDataFiltered(c, info);
            if (result != null) return result;
            return findHardwareByDataFullScan(c, info);
        } catch (SQLException e) {
            logger.error("SQL error in getHardwareInfoByData", e);
        }
        return null;
    }

    private SQLUserHardware findHardwareByDataFiltered(Connection c, HardwareReportRequest.HardwareInfo info) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sqlFindHardwareByDataFiltered)) {
            s.setString(1, info.hwDiskId);
            s.setString(2, info.baseboardSerialNumber);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    SQLUserHardware hw = mapHardware(rs);
                    if (compareHardwareInfo(hw.getHardwareInfo(), info).compareLevel >= criticalCompareLevel) {
                        return hw;
                    }
                }
            }
        }
        return null;
    }

    private SQLUserHardware findHardwareByDataFullScan(Connection c, HardwareReportRequest.HardwareInfo info) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sqlFindHardwareByData);
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) {
                SQLUserHardware hw = mapHardware(rs);
                if (compareHardwareInfo(hw.getHardwareInfo(), info).compareLevel >= criticalCompareLevel) {
                    return hw;
                }
            }
        }
        return null;
    }

    @Override
    public UserHardware getHardwareInfoById(String id) {
        try (Connection c = holder.getConnection();
             PreparedStatement s = c.prepareStatement(sqlFindHardwareById)) {
            s.setLong(1, Long.parseLong(id));
            try (ResultSet rs = s.executeQuery()) {
                return rs.next() ? mapHardware(rs) : null;
            }
        } catch (SQLException e) {
            logger.error("SQL error in getHardwareInfoById", e);
            return null;
        }
    }

    @Override
    public UserHardware createHardwareInfo(HardwareReportRequest.HardwareInfo info, byte[] publicKey) {
        try (Connection c = holder.getConnection()) {
            c.setAutoCommit(false);
            long generatedId;
            try (PreparedStatement s = c.prepareStatement(sqlCreateHardware, Statement.RETURN_GENERATED_KEYS)) {
                s.setBytes(1, publicKey);
                s.setString(2, info.hwDiskId);
                s.setString(3, info.baseboardSerialNumber);
                s.setBytes(4, info.displayId);
                s.setInt(5, info.bitness);
                s.setLong(6, info.totalMemory);
                s.setInt(7, info.logicalProcessors);
                s.setInt(8, info.physicalProcessors);
                s.setLong(9, info.processorMaxFreq);
                s.setString(10, info.graphicCard);
                s.setBoolean(11, info.battery);
                s.executeUpdate();

                try (ResultSet keys = s.getGeneratedKeys()) {
                    if (!keys.next()) { c.rollback(); return null; }
                    generatedId = keys.getLong(1);
                }
            }
            logHwidCreation(c, generatedId, publicKey);
            c.commit();
            return new SQLUserHardware(info, publicKey, generatedId, false);
        } catch (SQLException e) {
            logger.error("SQL error in createHardwareInfo", e);
            return null;
        }
    }

    @Override
    public void connectUserAndHardware(UserSession userSession, UserHardware hardware) {
        SQLUser user         = (SQLUser) userSession.getUser();
        SQLUserHardware hwid = (SQLUserHardware) hardware;
        if (user.hwidId == hwid.id) return;
        user.hwidId = hwid.id;
        try (Connection c = holder.getConnection();
             PreparedStatement s = c.prepareStatement(sqlUpdateUserHwidId)) {
            s.setLong(1, hwid.id);
            s.setString(2, user.getUUID().toString());
            s.executeUpdate();
        } catch (SQLException e) {
            logger.error("SQL error in connectUserAndHardware", e);
        }
    }

    @Override
    public void addPublicKeyToHardwareInfo(UserHardware hardware, byte[] publicKey) {
        SQLUserHardware hwid = (SQLUserHardware) hardware;
        hwid.publicKey = publicKey;
        try (Connection c = holder.getConnection();
             PreparedStatement s = c.prepareStatement(sqlUpdateHardwarePublicKey)) {
            s.setBytes(1, publicKey);
            s.setLong(2, hwid.id);
            s.executeUpdate();
        } catch (SQLException e) {
            logger.error("SQL error in addPublicKeyToHardwareInfo", e);
        }
    }

    @Override
    public Iterable<User> getUsersByHardwareInfo(UserHardware hardware) {
        List<User> users = new ArrayList<>();
        try (Connection c = holder.getConnection();
             PreparedStatement s = c.prepareStatement(sqlUsersByHwidId)) {
            s.setLong(1, Long.parseLong(hardware.getId()));
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    SQLUser user = constructUserFromRow(rs);
                    user.permissions = loadPermissions(c, user.uuid.toString());
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            logger.error("SQL error in getUsersByHardwareInfo", e);
            return null;
        }
        return users;
    }

    @Override
    public void banHardware(UserHardware hardware) {
        setHardwareBanned(hardware, true);
    }

    @Override
    public void unbanHardware(UserHardware hardware) {
        setHardwareBanned(hardware, false);
    }

    // -------------------------------------------------------------------------
    // AuthSupportExtendedCheckServer
    // -------------------------------------------------------------------------

    @Override
    public UserSession extendedCheckServer(Client client, String username, String serverID) {
        SQLUser user = (SQLUser) getUserByUsername(username);
        if (user == null) return null;
        return user.getUsername().equals(username) && user.getServerId().equals(serverID)
                ? createSession(user)
                : null;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String hardwareColumns() {
        return "id, hwDiskId, baseboardSerialNumber, displayId, bitness, totalMemory, " +
                "logicalProcessors, physicalProcessors, processorMaxFreq, battery, " +
                "graphicCard, banned, publicKey";
    }

    /** Maps the <em>current</em> row of {@code rs} to a {@link SQLUserHardware}. */
    private static SQLUserHardware mapHardware(ResultSet rs) throws SQLException {
        HardwareReportRequest.HardwareInfo info = new HardwareReportRequest.HardwareInfo();
        info.hwDiskId              = rs.getString("hwDiskId");
        info.baseboardSerialNumber = rs.getString("baseboardSerialNumber");
        info.displayId             = rs.getBytes("displayId");
        info.bitness               = rs.getInt("bitness");
        info.totalMemory           = rs.getLong("totalMemory");
        info.logicalProcessors     = rs.getInt("logicalProcessors");
        info.physicalProcessors    = rs.getInt("physicalProcessors");
        info.processorMaxFreq      = rs.getLong("processorMaxFreq");
        info.battery               = rs.getBoolean("battery");
        info.graphicCard           = rs.getString("graphicCard");
        return new SQLUserHardware(
                info, rs.getBytes("publicKey"), rs.getLong("id"), rs.getBoolean("banned"));
    }

    private void setHardwareBanned(UserHardware hardware, boolean banned) {
        SQLUserHardware hwid = (SQLUserHardware) hardware;
        hwid.banned = banned;
        try (Connection c = holder.getConnection();
             PreparedStatement s = c.prepareStatement(sqlUpdateHardwareBanned)) {
            s.setBoolean(1, banned);
            s.setLong(2, hwid.id);
            s.executeUpdate();
        } catch (SQLException e) {
            logger.error("SQL error while {} hardware id={}",
                    banned ? "banning" : "unbanning", hwid.id, e);
        }
    }

    private void logHwidCreation(Connection c, long hwidId, byte[] publicKey) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sqlCreateHWIDLog)) {
            s.setLong(1, hwidId);
            s.setBytes(2, publicKey);
            s.executeUpdate();
        }
    }

    private static String resolve(String override, String fallback) {
        return override != null ? override : fallback;
    }

    // =========================================================================
    // Inner types
    // =========================================================================

    /** Session with lazy-loaded hardware info. */
    public class SQLUserSession extends AbstractSQLCoreProvider.SQLUserSession
            implements UserSessionSupportHardware {

        private final transient SQLUser sqlUser;
        private transient SQLUserHardware cachedHardware;

        public SQLUserSession(AbstractSQLCoreProvider.SQLUser user) {
            super(user);
            this.sqlUser = (SQLUser) user;
        }

        @Override
        public String getHardwareId() {
            return sqlUser.hwidId == 0 ? null : String.valueOf(sqlUser.hwidId);
        }

        @Override
        public UserHardware getHardware() {
            if (cachedHardware == null && sqlUser.hwidId != 0) {
                cachedHardware = (SQLUserHardware) getHardwareInfoById(String.valueOf(sqlUser.hwidId));
            }
            return cachedHardware;
        }
    }

    @Override
    protected SQLUserSession createSession(AbstractSQLCoreProvider.SQLUser user) {
        return new SQLUserSession(user);
    }

    /** Immutable hardware record (except mutable {@code publicKey} and {@code banned}). */
    public static class SQLUserHardware implements UserHardware {
        private final HardwareReportRequest.HardwareInfo hardwareInfo;
        private final long id;
        private byte[] publicKey;
        private boolean banned;

        public SQLUserHardware(HardwareReportRequest.HardwareInfo hardwareInfo,
                               byte[] publicKey, long id, boolean banned) {
            this.hardwareInfo = hardwareInfo;
            this.publicKey    = publicKey;
            this.id           = id;
            this.banned       = banned;
        }

        @Override public HardwareReportRequest.HardwareInfo getHardwareInfo() { return hardwareInfo; }
        @Override public byte[] getPublicKey()                                 { return publicKey; }
        @Override public String getId()                                        { return String.valueOf(id); }
        @Override public boolean isBanned()                                    { return banned; }

        @Override
        public String toString() {
            return "SQLUserHardware{id=%d, banned=%b, publicKey=%s, info=%s}".formatted(
                    id, banned,
                    publicKey == null ? "null" : Base64.getEncoder().encodeToString(publicKey),
                    hardwareInfo);
        }
    }

    public static class SQLUser extends AbstractSQLCoreProvider.SQLUser {
        protected long hwidId;

        public SQLUser(UUID uuid, String username, String accessToken,
                       String serverId, String password, long hwidId) {
            super(uuid, username, accessToken, serverId, password);
            this.hwidId = hwidId;
        }

        @Override
        public String toString() {
            return "SQLUser{uuid=%s, username='%s', permissions=%s, hwidId=%d}"
                    .formatted(uuid, username, permissions, hwidId);
        }
    }
}