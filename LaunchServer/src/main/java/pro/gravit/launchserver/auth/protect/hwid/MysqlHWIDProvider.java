package pro.gravit.launchserver.auth.protect.hwid;

import pro.gravit.launcher.request.secure.HardwareReportRequest;
import pro.gravit.launchserver.auth.MySQLSourceConfig;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.*;

public class MysqlHWIDProvider extends HWIDProvider {
    public MySQLSourceConfig mySQLHolder;
    public double warningSpoofingLevel = -1.0;
    public double criticalCompareLevel = 1.0;

    @Override
    public void init() {

    }

    @Override
    public HardwareReportRequest.HardwareInfo findHardwareInfoByPublicKey(byte[] publicKey) throws HWIDException {
        try(Connection connection = mySQLHolder.getConnection())
        {
            PreparedStatement s = connection.prepareStatement("SELECT hwDiskId, baseboardSerialNumber, displayId, bitness, totalMemory, logicalProcessors, physicalProcessors, processorMaxFreq, battery, banned FROM hwids WHERE `publicKey` = ?");
            s.setBlob(1, new ByteArrayInputStream(publicKey));
            ResultSet set = s.executeQuery();
            if(set.next())
            {
                if(set.getBoolean(10)) //isBanned
                {
                    throw new SecurityException("You HWID banned");
                }
                return fetchHardwareInfo(set);
            }
            else
            {
                return null;
            }
        } catch (SQLException | IOException throwables) {
            LogHelper.error(throwables);
            throw new SecurityException("SQL error. Please try again later");
        }
    }

    private HardwareReportRequest.HardwareInfo fetchHardwareInfo(ResultSet set) throws SQLException, IOException {
        HardwareReportRequest.HardwareInfo hardwareInfo = new HardwareReportRequest.HardwareInfo();
        hardwareInfo.hwDiskId = set.getString(1);
        hardwareInfo.baseboardSerialNumber = set.getString(2);
        hardwareInfo.displayId = IOHelper.read(set.getBlob(3).getBinaryStream());
        hardwareInfo.bitness = set.getInt(4);
        hardwareInfo.totalMemory = set.getLong(5);
        hardwareInfo.logicalProcessors = set.getInt(6);
        hardwareInfo.physicalProcessors = set.getInt(7);
        hardwareInfo.processorMaxFreq = set.getLong(8);
        hardwareInfo.battery = set.getBoolean(9);
        return hardwareInfo;
    }

    @Override
    public void createHardwareInfo(HardwareReportRequest.HardwareInfo hardwareInfo, byte[] publicKey) throws HWIDException {
        try(Connection connection = mySQLHolder.getConnection())
        {
            PreparedStatement s = connection.prepareStatement("INSERT INTO `hwids` (`publickey`, `hwDiskId`, `baseboardSerialNumber`, `displayId`, `bitness`, `totalMemory`, `logicalProcessors`, `physicalProcessors`, `processorMaxFreq`, `battery`, `banned`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0');", Statement.RETURN_GENERATED_KEYS);
            s.setBlob(1, new ByteArrayInputStream(publicKey));
            s.setString(2, hardwareInfo.hwDiskId);
            s.setString(3, hardwareInfo.baseboardSerialNumber);
            s.setBlob(4, new ByteArrayInputStream(hardwareInfo.displayId));
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
                }
            }
        } catch (SQLException throwables) {
            LogHelper.error(throwables);
            throw new SecurityException("SQL error. Please try again later");
        }
    }

    @Override
    public boolean addPublicKeyToHardwareInfo(HardwareReportRequest.HardwareInfo hardwareInfo, byte[] publicKey) throws HWIDException {
        try(Connection connection = mySQLHolder.getConnection())
        {
            PreparedStatement s = connection.prepareStatement("SELECT hwDiskId, baseboardSerialNumber, displayId, bitness, totalMemory, logicalProcessors, physicalProcessors, processorMaxFreq, battery, id, banned FROM hwids");
            ResultSet set = s.executeQuery();
            while(set.next())
            {
                HardwareReportRequest.HardwareInfo hw = fetchHardwareInfo(set);
                long id = set.getLong(10);
                HardwareInfoCompareResult result = compareHardwareInfo(hw, hardwareInfo);
                if(result.compareLevel > criticalCompareLevel)
                {
                    if(set.getBoolean(11)) //isBanned
                    {
                        throw new SecurityException("You HWID banned");
                    }
                    writeHwidLog(connection, id, publicKey);
                    changePublicKey(connection, id, publicKey);
                    return true;
                }
            }
        } catch (SQLException | IOException throwables)
        {
            LogHelper.error(throwables);
            throw new SecurityException("SQL error. Please try again later");
        }
        return false;
    }
    private void changePublicKey(Connection connection, long id, byte[] publicKey) throws SQLException {
        PreparedStatement s = connection.prepareStatement("UPDATE hwids SET `publicKey` = ? WHERE `id` = ?");
        s.setBlob(1, new ByteArrayInputStream(publicKey));
        s.setLong(2, id);
        s.executeUpdate();
    }
    private void writeHwidLog(Connection connection, long hwidId, byte[] newPublicKey) throws SQLException {
        PreparedStatement s = connection.prepareStatement("INSERT INTO hwidLog (`hwidId`, `newPublicKey`) VALUES (?, ?)");
        s.setLong(1, hwidId);
        s.setBlob(2, new ByteArrayInputStream(newPublicKey));
        s.executeUpdate();
    }

    @Override
    public void close() {
        mySQLHolder.close();
    }
}
