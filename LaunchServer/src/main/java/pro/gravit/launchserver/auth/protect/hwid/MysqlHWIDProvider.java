package pro.gravit.launchserver.auth.protect.hwid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.request.secure.HardwareReportRequest;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.MySQLSourceConfig;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.utils.helper.IOHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.*;

public class MysqlHWIDProvider extends HWIDProvider {
    private transient final Logger logger = LogManager.getLogger();
    public MySQLSourceConfig mySQLHolder;
    public double warningSpoofingLevel = -1.0;
    public double criticalCompareLevel = 1.0;
    public String tableHWID = "hwids";
    public String tableHWIDLog = "hwidLog";
    public String tableUsers;
    public String usersNameColumn;
    public String usersHWIDColumn;
    private String sqlFindByPublicKey;
    private String sqlFindByHardware;
    private String sqlCreateHardware;
    private String sqlCreateHWIDLog;
    private String sqlUpdateHardware;
    private String sqlUpdateUsers;

    @Override
    public void init(LaunchServer server) {
        sqlFindByPublicKey = String.format("SELECT hwDiskId, baseboardSerialNumber, displayId, bitness, totalMemory, logicalProcessors, physicalProcessors, processorMaxFreq, battery, id, banned FROM %s WHERE `publicKey` = ?", tableHWID);
        sqlFindByHardware = String.format("SELECT hwDiskId, baseboardSerialNumber, displayId, bitness, totalMemory, logicalProcessors, physicalProcessors, processorMaxFreq, battery, id, banned FROM %s", tableHWID);
        sqlCreateHardware = String.format("INSERT INTO `%s` (`publickey`, `hwDiskId`, `baseboardSerialNumber`, `displayId`, `bitness`, `totalMemory`, `logicalProcessors`, `physicalProcessors`, `processorMaxFreq`, `battery`, `banned`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0')", tableHWID);
        sqlCreateHWIDLog = String.format("INSERT INTO %s (`hwidId`, `newPublicKey`) VALUES (?, ?)", tableHWIDLog);
        sqlUpdateHardware = String.format("UPDATE %s SET `publicKey` = ? WHERE `id` = ?", tableHWID);
        if (tableUsers != null && usersHWIDColumn != null && usersNameColumn != null) {
            sqlUpdateUsers = String.format("UPDATE %s SET `%s` = ? WHERE `%s` = ?", tableUsers, usersHWIDColumn, usersNameColumn);
        } else {
            logger.warn("[MysqlHWIDProvider] Link to users table not configured");
        }
    }

    @Override
    public HardwareReportRequest.HardwareInfo findHardwareInfoByPublicKey(byte[] publicKey, Client client) throws HWIDException {
        try (Connection connection = mySQLHolder.getConnection()) {
            PreparedStatement s = connection.prepareStatement(sqlFindByPublicKey);
            s.setBlob(1, new ByteArrayInputStream(publicKey));
            ResultSet set = s.executeQuery();
            if (set.next()) {
                if (set.getBoolean(11)) //isBanned
                {
                    throw new HWIDException("You HWID banned");
                }
                long id = set.getLong(10);
                setUserHardwareId(connection, client.username, id);
                return fetchHardwareInfo(set);
            } else {
                return null;
            }
        } catch (SQLException | IOException throwables) {
            logger.error(throwables);
            throw new HWIDException("SQL error. Please try again later");
        }
    }

    private HardwareReportRequest.HardwareInfo fetchHardwareInfo(ResultSet set) throws SQLException, IOException {
        HardwareReportRequest.HardwareInfo hardwareInfo = new HardwareReportRequest.HardwareInfo();
        hardwareInfo.hwDiskId = set.getString(1);
        hardwareInfo.baseboardSerialNumber = set.getString(2);
        Blob displayId = set.getBlob(3);
        hardwareInfo.displayId = displayId == null ? null : IOHelper.read(displayId.getBinaryStream());
        hardwareInfo.bitness = set.getInt(4);
        hardwareInfo.totalMemory = set.getLong(5);
        hardwareInfo.logicalProcessors = set.getInt(6);
        hardwareInfo.physicalProcessors = set.getInt(7);
        hardwareInfo.processorMaxFreq = set.getLong(8);
        hardwareInfo.battery = set.getBoolean(9);
        return hardwareInfo;
    }

    @Override
    public void createHardwareInfo(HardwareReportRequest.HardwareInfo hardwareInfo, byte[] publicKey, Client client) throws HWIDException {
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
            s.setBoolean(10, hardwareInfo.battery);
            s.executeUpdate();
            try (ResultSet generatedKeys = s.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    writeHwidLog(connection, generatedKeys.getLong(1), publicKey);
                    setUserHardwareId(connection, client.username, generatedKeys.getLong(1));
                }
            }
        } catch (SQLException throwables) {
            logger.error(throwables);
            throw new HWIDException("SQL error. Please try again later");
        }
    }

    @Override
    public boolean addPublicKeyToHardwareInfo(HardwareReportRequest.HardwareInfo hardwareInfo, byte[] publicKey, Client client) throws HWIDException {
        try (Connection connection = mySQLHolder.getConnection()) {
            PreparedStatement s = connection.prepareStatement(sqlFindByHardware);
            ResultSet set = s.executeQuery();
            while (set.next()) {
                HardwareReportRequest.HardwareInfo hw = fetchHardwareInfo(set);
                long id = set.getLong(10);
                HardwareInfoCompareResult result = compareHardwareInfo(hw, hardwareInfo);
                if (result.compareLevel > criticalCompareLevel) {
                    if (set.getBoolean(11)) //isBanned
                    {
                        throw new HWIDException("You HWID banned");
                    }
                    writeHwidLog(connection, id, publicKey);
                    changePublicKey(connection, id, publicKey);
                    setUserHardwareId(connection, client.username, id);
                    return true;
                }
            }
        } catch (SQLException | IOException throwables) {
            logger.error(throwables);
            throw new HWIDException("SQL error. Please try again later");
        }
        return false;
    }

    private void changePublicKey(Connection connection, long id, byte[] publicKey) throws SQLException {
        PreparedStatement s = connection.prepareStatement(sqlUpdateHardware);
        s.setBlob(1, new ByteArrayInputStream(publicKey));
        s.setLong(2, id);
        s.executeUpdate();
    }

    private void writeHwidLog(Connection connection, long hwidId, byte[] newPublicKey) throws SQLException {
        PreparedStatement s = connection.prepareStatement(sqlCreateHWIDLog);
        s.setLong(1, hwidId);
        s.setBlob(2, new ByteArrayInputStream(newPublicKey));
        s.executeUpdate();
    }

    private void setUserHardwareId(Connection connection, String username, long hwidId) throws SQLException {
        if (sqlUpdateUsers == null || username == null) return;
        PreparedStatement s = connection.prepareStatement(sqlUpdateUsers);
        s.setLong(1, hwidId);
        s.setString(2, username);
        s.executeUpdate();
    }

    @Override
    public void close() {
        mySQLHolder.close();
    }
}
