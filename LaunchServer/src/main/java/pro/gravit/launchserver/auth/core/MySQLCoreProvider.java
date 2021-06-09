package pro.gravit.launchserver.auth.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launcher.request.secure.HardwareReportRequest;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.MySQLSourceConfig;
import pro.gravit.launchserver.auth.core.interfaces.UserHardware;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportHardware;
import pro.gravit.launchserver.auth.core.interfaces.user.UserSupportHardware;
import pro.gravit.launchserver.auth.password.PasswordVerifier;
import pro.gravit.launchserver.auth.protect.hwid.HWIDProvider;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class MySQLCoreProvider extends AuthCoreProvider implements AuthSupportHardware {
    private transient final Logger logger = LogManager.getLogger();
    public MySQLSourceConfig mySQLHolder;

    public String uuidColumn;
    public String usernameColumn;
    public String accessTokenColumn;
    public String passwordColumn;
    public String serverIDColumn;
    public String hardwareIdColumn;
    public String table;

    public String tableHWID = "hwids";
    public String tableHWIDLog = "hwidLog";
    private transient String sqlFindHardwareByPublicKey;
    private transient String sqlFindHardwareByData;
    private transient String sqlFindHardwareById;
    private transient String sqlCreateHardware;
    private transient String sqlCreateHWIDLog;
    private transient String sqlUpdateHardwarePublicKey;
    private transient String sqlUpdateHardwareBanned;
    private transient String sqlUpdateUsers;
    private transient String sqlUsersByHwidId;

    public PasswordVerifier passwordVerifier;
    public double criticalCompareLevel = 1.0;

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
    public UserSession getUserSessionByOAuthAccessToken(String accessToken) throws OAuthAccessTokenExpired {
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
        if (passwordVerifier.check(((MySQLUser) user).password, ((AuthPlainPassword) password).password)) {
            return new PasswordVerifyReport(true);
        } else {
            return PasswordVerifyReport.FAILED;
        }
    }

    @Override
    public AuthManager.AuthReport createOAuthSession(User user, AuthResponse.AuthContext context, PasswordVerifyReport report, boolean minecraftAccess) throws IOException {
        if (minecraftAccess) {
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
        if (hardwareIdColumn == null) logger.error("hardwareIdColumn cannot be null");
        if (table == null) logger.error("table cannot be null");
        // Prepare SQL queries
        String userInfoCols = String.format("%s, %s, %s, %s, %s, %s", uuidColumn, usernameColumn, accessTokenColumn, serverIDColumn, passwordColumn, hardwareIdColumn);
        queryByUUIDSQL = String.format("SELECT %s FROM %s WHERE %s=? LIMIT 1", userInfoCols,
                table, uuidColumn);
        queryByUsernameSQL = String.format("SELECT %s FROM %s WHERE %s=? LIMIT 1",
                userInfoCols, table, usernameColumn);

        updateAuthSQL = String.format("UPDATE %s SET %s=?, %s=NULL WHERE %s=? LIMIT 1",
                table, accessTokenColumn, serverIDColumn, uuidColumn);
        updateServerIDSQL = String.format("UPDATE %s SET %s=? WHERE %s=? LIMIT 1",
                table, serverIDColumn, uuidColumn);
        String hardwareInfoCols = "id, hwDiskId, baseboardSerialNumber, displayId, bitness, totalMemory, logicalProcessors, physicalProcessors, processorMaxFreq, battery, id, graphicCard, banned";
        sqlFindHardwareByPublicKey = String.format("SELECT %s FROM %s WHERE `publicKey` = ?", hardwareInfoCols, tableHWID);
        sqlFindHardwareById = String.format("SELECT %s FROM %s WHERE `id` = ?", hardwareInfoCols, tableHWID);
        sqlUsersByHwidId = String.format("SELECT %s FROM %s WHERE `%s` = ?", userInfoCols, table, hardwareIdColumn);
        sqlFindHardwareByData = String.format("SELECT %s FROM %s", hardwareInfoCols, tableHWID);
        sqlCreateHardware = String.format("INSERT INTO `%s` (`publickey`, `hwDiskId`, `baseboardSerialNumber`, `displayId`, `bitness`, `totalMemory`, `logicalProcessors`, `physicalProcessors`, `processorMaxFreq`, `battery`, `graphicCard`, `banned`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0')", tableHWID);
        sqlCreateHWIDLog = String.format("INSERT INTO %s (`hwidId`, `newPublicKey`) VALUES (?, ?)", tableHWIDLog);
        sqlUpdateHardwarePublicKey = String.format("UPDATE %s SET `publicKey` = ? WHERE `id` = ?", tableHWID);
        sqlUpdateHardwareBanned = String.format("UPDATE %s SET `banned` = ? WHERE `id` = ?", tableHWID);
        sqlUpdateUsers = String.format("UPDATE %s SET `%s` = ? WHERE `%s` = ?", table, hardwareIdColumn, uuidColumn);
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
                set.getString(accessTokenColumn), set.getString(serverIDColumn), set.getString(passwordColumn), new ClientPermissions(), set.getLong(hardwareIdColumn)) : null;
    }

    private MySQLUserHardware fetchHardwareInfo(ResultSet set) throws SQLException, IOException {
        HardwareReportRequest.HardwareInfo hardwareInfo = new HardwareReportRequest.HardwareInfo();
        hardwareInfo.hwDiskId = set.getString("hwDiskId");
        hardwareInfo.baseboardSerialNumber = set.getString("baseboardSerialNumber");
        Blob displayId = set.getBlob("displayId");
        hardwareInfo.displayId = displayId == null ? null : IOHelper.read(displayId.getBinaryStream());
        hardwareInfo.bitness = set.getInt("bitness");
        hardwareInfo.totalMemory = set.getLong("totalMemory");
        hardwareInfo.logicalProcessors = set.getInt("logicalProcessors");
        hardwareInfo.physicalProcessors = set.getInt("physicalProcessors");
        hardwareInfo.processorMaxFreq = set.getLong("processorMaxFreq");
        hardwareInfo.battery = set.getBoolean("battery");
        hardwareInfo.graphicCard = set.getString("graphicCard");
        Blob publicKey = set.getBlob("publicKey");
        long id = set.getLong("id");
        boolean banned = set.getBoolean("banned");
        return new MySQLUserHardware(hardwareInfo, publicKey == null ? null : IOHelper.read(publicKey.getBinaryStream()), id, banned);
    }

    private void setUserHardwareId(Connection connection, UUID uuid, long hwidId) throws SQLException {
        PreparedStatement s = connection.prepareStatement(sqlUpdateUsers);
        s.setLong(1, hwidId);
        s.setString(2, uuid.toString());
        s.executeUpdate();
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

    @Override
    public UserHardware getHardwareInfoByPublicKey(byte[] publicKey) {
        try (Connection connection = mySQLHolder.getConnection()) {
            PreparedStatement s = connection.prepareStatement(sqlFindHardwareByPublicKey);
            s.setBlob(1, new ByteArrayInputStream(publicKey));
            try (ResultSet set = s.executeQuery()) {
                if (set.next()) {
                    return fetchHardwareInfo(set);
                } else {
                    return null;
                }
            }
        } catch (SQLException | IOException throwables) {
            logger.error(throwables);
            return null;
        }
    }

    @Override
    public UserHardware getHardwareInfoByData(HardwareReportRequest.HardwareInfo info) {
        try (Connection connection = mySQLHolder.getConnection()) {
            PreparedStatement s = connection.prepareStatement(sqlFindHardwareByData);
            try (ResultSet set = s.executeQuery()) {
                while (set.next()) {
                    MySQLUserHardware hw = fetchHardwareInfo(set);
                    HWIDProvider.HardwareInfoCompareResult result = compareHardwareInfo(hw.getHardwareInfo(), info);
                    if (result.compareLevel > criticalCompareLevel) {
                        return hw;
                    }
                }
            }
        } catch (SQLException | IOException throwables) {
            logger.error(throwables);
        }
        return null;
    }

    @Override
    public UserHardware getHardwareInfoById(String id) {
        try (Connection connection = mySQLHolder.getConnection()) {
            PreparedStatement s = connection.prepareStatement(sqlFindHardwareById);
            s.setLong(1, Long.parseLong(id));
            try (ResultSet set = s.executeQuery()) {
                if (set.next()) {
                    return fetchHardwareInfo(set);
                } else {
                    return null;
                }
            }
        } catch (SQLException | IOException throwables) {
            logger.error(throwables);
            return null;
        }
    }

    @Override
    public UserHardware createHardwareInfo(HardwareReportRequest.HardwareInfo hardwareInfo, byte[] publicKey) {
        try (Connection connection = mySQLHolder.getConnection()) {
            PreparedStatement s = connection.prepareStatement(sqlCreateHardware, Statement.RETURN_GENERATED_KEYS);
            s.setBlob(1, new ByteArrayInputStream(publicKey));
            s.setString(2, hardwareInfo.hwDiskId);
            s.setString(3, hardwareInfo.baseboardSerialNumber);
            s.setBlob(4, hardwareInfo.displayId == null ? null : new ByteArrayInputStream(hardwareInfo.displayId));
            s.setInt(5, hardwareInfo.bitness);
            s.setLong(6, hardwareInfo.totalMemory);
            s.setInt(7, hardwareInfo.logicalProcessors);
            s.setInt(8, hardwareInfo.physicalProcessors);
            s.setLong(9, hardwareInfo.processorMaxFreq);
            s.setString(10, hardwareInfo.graphicCard);
            s.setBoolean(11, hardwareInfo.battery);
            s.executeUpdate();
            try (ResultSet generatedKeys = s.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    //writeHwidLog(connection, generatedKeys.getLong(1), publicKey);
                    long id = generatedKeys.getLong(1);
                    return new MySQLUserHardware(hardwareInfo, publicKey, id, false);
                }
            }
            return null;
        } catch (SQLException throwables) {
            logger.error(throwables);
            return null;
        }
    }

    @Override
    public void connectUserAndHardware(User user, UserHardware hardware) {
        MySQLUser mySQLUser = (MySQLUser) user;
        MySQLUserHardware mySQLUserHardware = (MySQLUserHardware) hardware;
        if (mySQLUser.hwidId == mySQLUserHardware.id) return;
        mySQLUser.hwidId = mySQLUserHardware.id;
        try (Connection connection = mySQLHolder.getConnection()) {
            setUserHardwareId(connection, user.getUUID(), mySQLUserHardware.id);
        } catch (SQLException throwables) {
            logger.error(throwables);
        }
    }

    @Override
    public void addPublicKeyToHardwareInfo(UserHardware hardware, byte[] publicKey) {
        MySQLUserHardware mySQLUserHardware = (MySQLUserHardware) hardware;
        mySQLUserHardware.publicKey = publicKey;
        try (Connection connection = mySQLHolder.getConnection()) {
            PreparedStatement s = connection.prepareStatement(sqlUpdateHardwarePublicKey);
            s.setBlob(1, new ByteArrayInputStream(publicKey));
            s.setLong(2, mySQLUserHardware.id);
            s.executeUpdate();
        } catch (SQLException e) {
            logger.error(e);
        }
    }

    @Override
    public Iterable<User> getUsersByHardwareInfo(UserHardware hardware) {
        List<User> users = new LinkedList<>();
        try (Connection c = mySQLHolder.getConnection()) {
            PreparedStatement s = c.prepareStatement(sqlUsersByHwidId);
            s.setLong(1, Long.parseLong(hardware.getId()));
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            try (ResultSet set = s.executeQuery()) {
                while (set.next()) {
                    users.add(constructUser(set));
                }
            }
        } catch (SQLException e) {
            logger.error(e);
            return null;
        }
        return users;
    }

    @Override
    public void banHardware(UserHardware hardware) {
        MySQLUserHardware mySQLUserHardware = (MySQLUserHardware) hardware;
        mySQLUserHardware.banned = true;
        try (Connection connection = mySQLHolder.getConnection()) {
            PreparedStatement s = connection.prepareStatement(sqlUpdateHardwareBanned);
            s.setBoolean(1, true);
            s.setLong(2, mySQLUserHardware.id);
            s.executeUpdate();
        } catch (SQLException e) {
            logger.error(e);
        }
    }

    @Override
    public void unbanHardware(UserHardware hardware) {
        MySQLUserHardware mySQLUserHardware = (MySQLUserHardware) hardware;
        mySQLUserHardware.banned = false;
        try (Connection connection = mySQLHolder.getConnection()) {
            PreparedStatement s = connection.prepareStatement(sqlUpdateHardwareBanned);
            s.setBoolean(1, false);
            s.setLong(2, mySQLUserHardware.id);
            s.executeUpdate();
        } catch (SQLException e) {
            logger.error(e);
        }
    }

    public class MySQLUser implements User, UserSupportHardware {
        protected UUID uuid;
        protected String username;
        protected String accessToken;
        protected String serverId;
        protected String password;
        protected ClientPermissions permissions;
        protected long hwidId;
        protected transient MySQLUserHardware hardware;

        public MySQLUser(UUID uuid, String username, String accessToken, String serverId, String password, ClientPermissions permissions, long hwidId) {
            this.uuid = uuid;
            this.username = username;
            this.accessToken = accessToken;
            this.serverId = serverId;
            this.password = password;
            this.permissions = permissions;
            this.hwidId = hwidId;
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
        public UserHardware getHardware() {
            if (hardware != null) return hardware;
            MySQLUserHardware result = (MySQLUserHardware) getHardwareInfoById(String.valueOf(hwidId));
            hardware = result;
            return result;
        }
    }

    public static class MySQLUserHardware implements UserHardware {
        private HardwareReportRequest.HardwareInfo hardwareInfo;
        private byte[] publicKey;
        private long id;
        private boolean banned;

        public MySQLUserHardware(HardwareReportRequest.HardwareInfo hardwareInfo, byte[] publicKey, long id, boolean banned) {
            this.hardwareInfo = hardwareInfo;
            this.publicKey = publicKey;
            this.id = id;
            this.banned = banned;
        }

        @Override
        public HardwareReportRequest.HardwareInfo getHardwareInfo() {
            return hardwareInfo;
        }

        @Override
        public byte[] getPublicKey() {
            return publicKey;
        }

        @Override
        public String getId() {
            return String.valueOf(id);
        }

        @Override
        public boolean isBanned() {
            return banned;
        }
    }
}
