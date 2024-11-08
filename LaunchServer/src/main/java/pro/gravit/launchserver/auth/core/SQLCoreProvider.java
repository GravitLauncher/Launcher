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
import pro.gravit.utils.helper.IOHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class SQLCoreProvider extends AbstractSQLCoreProvider implements AuthSupportHardware, AuthSupportExtendedCheckServer {
    public HikariSQLSourceConfig holder;

    @Override
    public void close() {
        super.close();
        holder.close();
    }

    @Override
    public SQLSourceConfig getSQLConfig() {
        return holder;
    }


    public String hardwareIdColumn;
    public String tableHWID = "hwids";
    public String tableHWIDLog = "hwidLog";
    public double criticalCompareLevel = 1.0;
    private transient String sqlFindHardwareByPublicKey;
    private transient String sqlFindHardwareByData;
    private transient String sqlFindHardwareById;
    private transient String sqlCreateHardware;
    private transient String sqlCreateHWIDLog;
    private transient String sqlUpdateHardwarePublicKey;
    private transient String sqlUpdateHardwareBanned;
    private transient String sqlUpdateUsers;
    private transient String sqlUsersByHwidId;

    @Override
    public void init(LaunchServer server, AuthProviderPair pair) {
        holder.init();
        super.init(server, pair);
        String userInfoCols = makeUserCols();
        String hardwareInfoCols = "id, hwDiskId, baseboardSerialNumber, displayId, bitness, totalMemory, logicalProcessors, physicalProcessors, processorMaxFreq, battery, id, graphicCard, banned, publicKey";
        if (sqlFindHardwareByPublicKey == null)
            sqlFindHardwareByPublicKey = "SELECT %s FROM %s WHERE publicKey = ?".formatted(hardwareInfoCols, tableHWID);
        if (sqlFindHardwareById == null)
            sqlFindHardwareById = "SELECT %s FROM %s WHERE id = ?".formatted(hardwareInfoCols, tableHWID);
        if (sqlUsersByHwidId == null)
            sqlUsersByHwidId = "SELECT %s FROM %s WHERE %s = ?".formatted(userInfoCols, table, hardwareIdColumn);
        if (sqlFindHardwareByData == null)
            sqlFindHardwareByData = "SELECT %s FROM %s".formatted(hardwareInfoCols, tableHWID);
        if (sqlCreateHardware == null)
            sqlCreateHardware = "INSERT INTO %s (publickey, hwDiskId, baseboardSerialNumber, displayId, bitness, totalMemory, logicalProcessors, physicalProcessors, processorMaxFreq, graphicCard, battery, banned) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0')".formatted(tableHWID);
        if (sqlCreateHWIDLog == null)
            sqlCreateHWIDLog = "INSERT INTO %s (hwidId, newPublicKey) VALUES (?, ?)".formatted(tableHWIDLog);
        if (sqlUpdateHardwarePublicKey == null)
            sqlUpdateHardwarePublicKey = "UPDATE %s SET publicKey = ? WHERE id = ?".formatted(tableHWID);
        sqlUpdateHardwareBanned = "UPDATE %s SET banned = ? WHERE id = ?".formatted(tableHWID);
        sqlUpdateUsers = "UPDATE %s SET %s = ? WHERE %s = ?".formatted(table, hardwareIdColumn, uuidColumn);
    }

    @Override
    protected String makeUserCols() {
        return super.makeUserCols().concat(", ").concat(hardwareIdColumn);
    }

    @Override
    protected SQLUser constructUser(ResultSet set) throws SQLException {
        return set.next() ? new SQLUser(UUID.fromString(set.getString(uuidColumn)), set.getString(usernameColumn),
                set.getString(accessTokenColumn), set.getString(serverIDColumn), set.getString(passwordColumn), set.getLong(hardwareIdColumn)) : null;
    }

    private SQLUserHardware fetchHardwareInfo(ResultSet set) throws SQLException, IOException {
        HardwareReportRequest.HardwareInfo hardwareInfo = new HardwareReportRequest.HardwareInfo();
        hardwareInfo.hwDiskId = set.getString("hwDiskId");
        hardwareInfo.baseboardSerialNumber = set.getString("baseboardSerialNumber");
        byte[] displayId = set.getBytes("displayId");
        hardwareInfo.displayId = displayId == null ? null : displayId;
        hardwareInfo.bitness = set.getInt("bitness");
        hardwareInfo.totalMemory = set.getLong("totalMemory");
        hardwareInfo.logicalProcessors = set.getInt("logicalProcessors");
        hardwareInfo.physicalProcessors = set.getInt("physicalProcessors");
        hardwareInfo.processorMaxFreq = set.getLong("processorMaxFreq");
        hardwareInfo.battery = set.getBoolean("battery");
        hardwareInfo.graphicCard = set.getString("graphicCard");
        byte[] publicKey = set.getBytes("publicKey");
        long id = set.getLong("id");
        boolean banned = set.getBoolean("banned");
        return new SQLUserHardware(hardwareInfo, publicKey, id, banned);
    }

    private void setUserHardwareId(Connection connection, UUID uuid, long hwidId) throws SQLException {
        PreparedStatement s = connection.prepareStatement(sqlUpdateUsers);
        s.setLong(1, hwidId);
        s.setString(2, uuid.toString());
        s.executeUpdate();
    }

    @Override
    public UserHardware getHardwareInfoByPublicKey(byte[] publicKey) {
        try (Connection connection = holder.getConnection()) {
            connection.setAutoCommit(false);
            PreparedStatement s = connection.prepareStatement(sqlFindHardwareByPublicKey);
            s.setBytes(1, publicKey);
            try (ResultSet set = s.executeQuery()) {
                if (set.next()) {
                    connection.commit();
                    return fetchHardwareInfo(set);
                } else {
                    connection.commit();
                    return null;
                }
            }
        } catch (SQLException | IOException throwables) {
            logger.error("SQL Error", throwables);
            return null;
        }
    }

    @Override
    public UserHardware getHardwareInfoByData(HardwareReportRequest.HardwareInfo info) {
        try (Connection connection = holder.getConnection()) {
            connection.setAutoCommit(false);
            PreparedStatement s = connection.prepareStatement(sqlFindHardwareByData);
            try (ResultSet set = s.executeQuery()) {
                while (set.next()) {
                    SQLUserHardware hw = fetchHardwareInfo(set);
                    AuthSupportHardware.HardwareInfoCompareResult result = compareHardwareInfo(hw.getHardwareInfo(), info);
                    if (result.compareLevel > criticalCompareLevel) {
                        connection.commit();
                        return hw;
                    } else {
                        connection.commit();
                    }
                }
            }
        } catch (SQLException | IOException throwables) {
            logger.error("SQL Error", throwables);
        }
        return null;
    }

    @Override
    public UserHardware getHardwareInfoById(String id) {
        try (Connection connection = holder.getConnection()) {
            connection.setAutoCommit(false);
            PreparedStatement s = connection.prepareStatement(sqlFindHardwareById);
            s.setLong(1, Long.parseLong(id));
            try (ResultSet set = s.executeQuery()) {
                if (set.next()) {
                    connection.commit();
                    return fetchHardwareInfo(set);
                } else {
                    connection.commit();
                    return null;
                }
            }
        } catch (SQLException | IOException throwables) {
            logger.error("SQL Error", throwables);
            return null;
        }
    }

    @Override
    public UserHardware createHardwareInfo(HardwareReportRequest.HardwareInfo hardwareInfo, byte[] publicKey) {
        try (Connection connection = holder.getConnection()) {
            connection.setAutoCommit(false);
            PreparedStatement s = connection.prepareStatement(sqlCreateHardware, Statement.RETURN_GENERATED_KEYS);
            s.setBytes(1, publicKey);
            s.setString(2, hardwareInfo.hwDiskId);
            s.setString(3, hardwareInfo.baseboardSerialNumber);
            s.setBytes(4, hardwareInfo.displayId == null ? null : hardwareInfo.displayId);
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
                    connection.commit();
                    return new SQLUserHardware(hardwareInfo, publicKey, id, false);
                }
            }
            connection.commit();
            return null;
        } catch (SQLException throwables) {
            logger.error("SQL Error", throwables);
            return null;
        }
    }

    @Override
    public void connectUserAndHardware(UserSession userSession, UserHardware hardware) {
        AbstractSQLCoreProvider.SQLUserSession SQLUserSession = (AbstractSQLCoreProvider.SQLUserSession) userSession;
        SQLUser SQLUser = (SQLUser) SQLUserSession.getUser();
        SQLUserHardware SQLUserHardware = (SQLUserHardware) hardware;
        if (SQLUser.hwidId == SQLUserHardware.id) return;
        SQLUser.hwidId = SQLUserHardware.id;
        try (Connection connection = holder.getConnection()) {
            setUserHardwareId(connection, SQLUser.getUUID(), SQLUserHardware.id);
        } catch (SQLException throwables) {
            logger.error("SQL Error", throwables);
        }
    }

    @Override
    public void addPublicKeyToHardwareInfo(UserHardware hardware, byte[] publicKey) {
        SQLUserHardware SQLUserHardware = (SQLUserHardware) hardware;
        SQLUserHardware.publicKey = publicKey;
        try (Connection connection = holder.getConnection()) {
            connection.setAutoCommit(false);
            PreparedStatement s = connection.prepareStatement(sqlUpdateHardwarePublicKey);
            s.setBytes(1, publicKey);
            s.setLong(2, SQLUserHardware.id);
            s.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            logger.error("SQL error", e);
        }
    }

    @Override
    public Iterable<User> getUsersByHardwareInfo(UserHardware hardware) {
        List<User> users = new LinkedList<>();
        try (Connection c = holder.getConnection()) {
            c.setAutoCommit(false);
            PreparedStatement s = c.prepareStatement(sqlUsersByHwidId);
            s.setLong(1, Long.parseLong(hardware.getId()));
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            try (ResultSet set = s.executeQuery()) {
                while (!set.isLast()) {
                    users.add(constructUser(set));
                }
            }
            c.commit();
        } catch (SQLException e) {
            logger.error("SQL error", e);
            return null;
        }
        return users;
    }

    @Override
    public void banHardware(UserHardware hardware) {
        SQLUserHardware SQLUserHardware = (SQLUserHardware) hardware;
        SQLUserHardware.banned = true;
        try (Connection connection = holder.getConnection()) {
            PreparedStatement s = connection.prepareStatement(sqlUpdateHardwareBanned);
            s.setBoolean(1, true);
            s.setLong(2, SQLUserHardware.id);
            s.executeUpdate();
        } catch (SQLException e) {
            logger.error("SQL Error", e);
        }
    }

    @Override
    public void unbanHardware(UserHardware hardware) {
        SQLUserHardware SQLUserHardware = (SQLUserHardware) hardware;
        SQLUserHardware.banned = false;
        try (Connection connection = holder.getConnection()) {
            PreparedStatement s = connection.prepareStatement(sqlUpdateHardwareBanned);
            s.setBoolean(1, false);
            s.setLong(2, SQLUserHardware.id);
            s.executeUpdate();
        } catch (SQLException e) {
            logger.error("SQL error", e);
        }
    }

    @Override
    protected AbstractSQLCoreProvider.SQLUserSession createSession(AbstractSQLCoreProvider.SQLUser user) {
        return new SQLUserSession(user);
    }

    @Override
    public UserSession extendedCheckServer(Client client, String username, String serverID) throws IOException {
        AbstractSQLCoreProvider.SQLUser user = (AbstractSQLCoreProvider.SQLUser) getUserByUsername(username);
        if (user == null) {
            return null;
        }
        if (user.getUsername().equals(username) && user.getServerId().equals(serverID)) {
            return createSession(user);
        }
        return null;
    }

    public class SQLUserSession extends AbstractSQLCoreProvider.SQLUserSession implements UserSessionSupportHardware {
        private transient SQLUser SQLUser;
        protected transient SQLUserHardware hardware;

        public SQLUserSession(AbstractSQLCoreProvider.SQLUser user) {
            super(user);
            SQLUser = (SQLUser) user;
        }

        @Override
        public String getHardwareId() {
            return SQLUser.hwidId == 0 ? null : String.valueOf(SQLUser.hwidId);
        }

        @Override
        public UserHardware getHardware() {
            if(hardware == null) {
                hardware = (SQLUserHardware) getHardwareInfoById(String.valueOf(SQLUser.hwidId));
            }
            return hardware;
        }
    }

    public static class SQLUserHardware implements UserHardware {
        private final HardwareReportRequest.HardwareInfo hardwareInfo;
        private final long id;
        private byte[] publicKey;
        private boolean banned;

        public SQLUserHardware(HardwareReportRequest.HardwareInfo hardwareInfo, byte[] publicKey, long id, boolean banned) {
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

        @Override
        public String toString() {
            return "SQLUserHardware{" +
                    "hardwareInfo=" + hardwareInfo +
                    ", publicKey=" + (publicKey == null ? null : new String(Base64.getEncoder().encode(publicKey))) +
                    ", id=" + id +
                    ", banned=" + banned +
                    '}';
        }
    }

    public static class SQLUser extends AbstractSQLCoreProvider.SQLUser {
        protected long hwidId;

        public SQLUser(UUID uuid, String username, String accessToken, String serverId, String password, long hwidId) {
            super(uuid, username, accessToken, serverId, password);
            this.hwidId = hwidId;
        }

        @Override
        public String toString() {
            return "SQLUser{" +
                    "uuid=" + uuid +
                    ", username='" + username + '\'' +
                    ", permissions=" + permissions +
                    ", hwidId=" + hwidId +
                    '}';
        }
    }
}
