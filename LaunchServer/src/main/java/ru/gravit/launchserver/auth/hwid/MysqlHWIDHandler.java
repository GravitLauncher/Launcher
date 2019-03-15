package ru.gravit.launchserver.auth.hwid;

import ru.gravit.launcher.HWID;
import ru.gravit.launcher.OshiHWID;
import ru.gravit.launchserver.auth.MySQLSourceConfig;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.LogHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MysqlHWIDHandler extends HWIDHandler {
    private MySQLSourceConfig mySQLHolder;

    private String tableUsers;
    private String tableHwids;

    private String userFieldHwid;
    private String userFieldLogin;

    private String hwidFieldTotalMemory;
    private String hwidFieldSerialNumber;
    private String hwidFieldHWDiskSerial;
    private String hwidFieldProcessorID;
    private String hwidFieldBanned;

    private String queryHwids;
    private String[] paramsHwids;

    private String queryBan;
    private String[] paramsBan;

    private String banMessage;

    private boolean compareMode = false;
    //Using queryHWID "queryHwids": "SELECT * FROM `users_hwids` WHERE `totalMemory` = ? or `serialNumber` = ? or `HWDiskSerial` = ? or `processorID` = ?"
    private int compare = 50; //При наборе схожести в 50 очков
    private boolean oneCompareMode = false;

    /*
        //Добавить поля hwid в базу с пользователями

        //Создание таблицы для хранения HWID
        CREATE TABLE `fc_user_hwids` (
          `id` int(16) NOT NULL,
          `totalMemory` varchar(32) NOT NULL,
          `serialNumber` varchar(64) NOT NULL,
          `HWDiskSerial` varchar(64) NOT NULL,
          `processorID` varchar(64) NOT NULL,
          `isBanned` tinyint(1) NOT NULL DEFAULT '0'
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        ALTER TABLE `fc_user_hwids` ADD UNIQUE KEY `id` (`id`);

        ALTER TABLE `fc_user_hwids` MODIFY `id` int(16) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;
     */

    @Override
    public void check0(HWID hwid, String username) throws HWIDException {
        if (hwid instanceof OshiHWID) {
            OshiHWID oshiHWID = (OshiHWID) hwid;
            try(Connection c = mySQLHolder.getConnection()) {

                PreparedStatement s = c.prepareStatement(String.format("SELECT %s, %s FROM `%s` WHERE `%s` = ? LIMIT 1",
                        userFieldHwid, userFieldLogin, tableUsers, userFieldLogin));
                s.setString(1, username);

                // Execute SQL query
                s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
                try (ResultSet set = s.executeQuery()) {
                    if (set.next()) {
                        int hwid_id = set.getInt(userFieldHwid);
                        if (hwid_id == 0) {
                            onUpdateInfo(oshiHWID, username, c);
                        } else {
                            onCheckInfo(oshiHWID, username, c);
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void onUpdateInfo(OshiHWID hwid, String username, Connection c) throws HWIDException {
        try (PreparedStatement a = c.prepareStatement(queryHwids)) {

            String[] replaceParams = {"totalMemory", String.valueOf(hwid.totalMemory), "serialNumber", hwid.serialNumber, "HWDiskSerial", hwid.HWDiskSerial, "processorID", hwid.processorID};
            for (int i = 0; i < paramsHwids.length; i++) {
                a.setString(i + 1, CommonHelper.replace(paramsHwids[i], replaceParams));
            }

            ResultSet set = a.executeQuery();
            PreparedStatement ps;
            if (set.next()) {
                int id = set.getInt("id");
                boolean isBanned = set.getBoolean(hwidFieldBanned);

                ps = c.prepareStatement(String.format("UPDATE `%s` SET `%s` = ? WHERE `%s` = ?",
                        tableUsers, userFieldHwid, userFieldLogin));
                ps.setInt(1, id);
                ps.setString(2, username);

                ps.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
                ps.executeUpdate();

                if (isBanned) {
                    throw new HWIDException(banMessage);
                }
            } else {
                ps = c.prepareStatement(String.format("INSERT INTO `%s` (`%s`, `%s`, `%s`, `%s`) VALUES (?, ?, ?, ?);",
                        tableHwids, hwidFieldTotalMemory, hwidFieldSerialNumber, hwidFieldHWDiskSerial, hwidFieldProcessorID));
                ps.setString(1, String.valueOf(hwid.totalMemory));
                ps.setString(2, hwid.serialNumber);
                ps.setString(3, hwid.HWDiskSerial);
                ps.setString(4, hwid.processorID);
                ps.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
                ps.executeUpdate();

                ps = c.prepareStatement(String.format("UPDATE `%s` SET `%s` = LAST_INSERT_ID() WHERE `%s` = ?;",
                        tableUsers, userFieldHwid, userFieldLogin));
                ps.setString(1, username);
                ps.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void onCheckInfo(OshiHWID hwid, String username, Connection c) throws HWIDException {
        try (PreparedStatement a = c.prepareStatement(queryHwids)) {
            String[] replaceParams = {"totalMemory", String.valueOf(hwid.totalMemory), "serialNumber", hwid.serialNumber, "HWDiskSerial", hwid.HWDiskSerial, "processorID", hwid.processorID};
            for (int i = 0; i < paramsHwids.length; i++) {
                a.setString(i + 1, CommonHelper.replace(paramsHwids[i], replaceParams));
            }
            ResultSet set = a.executeQuery();
            boolean isOne = false;
            while(set.next()) {
                if(!oneCompareMode) isOne = true;
                if(compareMode)
                {
                    OshiHWID db_hwid = new OshiHWID();
                    db_hwid.serialNumber = set.getString(hwidFieldSerialNumber);
                    db_hwid.processorID = set.getString(hwidFieldProcessorID);
                    db_hwid.HWDiskSerial = set.getString(hwidFieldHWDiskSerial);
                    db_hwid.totalMemory = Long.valueOf(set.getString(hwidFieldTotalMemory));
                    db_hwid.macAddr = "";
                    LogHelper.dev("Compare HWID: %s vs %s", hwid.getSerializeString(), db_hwid.getSerializeString());
                    int compare_point = hwid.compare(db_hwid);
                    if(compare_point < compare) continue;
                    else
                    {
                        LogHelper.debug("User %s hwid check: found compare %d in %d", username, compare_point, set.getInt("id"));
                    }
                }
                if(oneCompareMode) isOne = true;
                boolean isBanned = set.getBoolean(hwidFieldBanned);
                if (isBanned) {
                    throw new HWIDException(banMessage);
                }
            }
            if(isOne) {
                onUpdateInfo(hwid, username, c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setIsBanned(HWID hwid, boolean isBanned) {
        LogHelper.debug("%s Request HWID: %s", isBanned ? "Ban" : "UnBan", hwid.toString());
        if (hwid instanceof OshiHWID) {
            OshiHWID oshiHWID = (OshiHWID) hwid;
            try(Connection c = mySQLHolder.getConnection()) {
                try (PreparedStatement a = c.prepareStatement(queryBan)) {
                    String[] replaceParamsUpd = {"totalMemory", String.valueOf(oshiHWID.totalMemory), "serialNumber", oshiHWID.serialNumber, "HWDiskSerial", oshiHWID.HWDiskSerial, "processorID", oshiHWID.processorID, "isBanned", isBanned ? "1" : "0"};
                    for (int i = 0; i < paramsBan.length; i++) {
                        a.setString(i + 1, CommonHelper.replace(paramsBan[i], replaceParamsUpd));
                    }
                    a.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
                    a.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void ban(List<HWID> list) {
        for (HWID hwid : list) {
            setIsBanned(hwid, true);
        }
    }

    @Override
    public void unban(List<HWID> list) {
        for (HWID hwid : list) {
            setIsBanned(hwid, false);
        }
    }

    @Override
    public List<HWID> getHwid(String username) {
        ArrayList<HWID> list = new ArrayList<>();
        try(Connection c = mySQLHolder.getConnection()) {
            LogHelper.debug("Try find HWID from username %s", username);
            PreparedStatement s = c.prepareStatement(String.format("SELECT %s, %s FROM `%s` WHERE `%s` = ? LIMIT 1", userFieldHwid, userFieldLogin, tableUsers, userFieldLogin));
            s.setString(1, username);

            // Execute SQL query
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);

            try (ResultSet set = s.executeQuery()) {
                if (set.next()) {
                    int hwid_id = set.getInt(userFieldHwid);
                    if (hwid_id != 0) {
                        s = c.prepareStatement(String.format("SELECT * FROM `%s` WHERE `id` = ? LIMIT 1", tableHwids));
                        s.setInt(1, hwid_id);
                        ResultSet rs = s.executeQuery();
                        if (rs.next()) {
                            OshiHWID oshiHWID = new OshiHWID();
                            oshiHWID.totalMemory = Long.valueOf(rs.getString(hwidFieldTotalMemory));
                            oshiHWID.serialNumber = rs.getString(hwidFieldSerialNumber);
                            oshiHWID.HWDiskSerial = rs.getString(hwidFieldHWDiskSerial);
                            oshiHWID.processorID = rs.getString(hwidFieldProcessorID);
                            list.add(oshiHWID);
                        }
                    }
                } else {
                    LogHelper.error(new HWIDException("HWID not found"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void close() {
    	mySQLHolder.close();
    }
}
