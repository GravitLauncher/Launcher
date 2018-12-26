package ru.gravit.launchserver.auth.hwid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ru.gravit.launcher.HWID;
import ru.gravit.launcher.OshiHWID;
import ru.gravit.launchserver.auth.MySQLSourceConfig;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.LogHelper;

public class MysqlHWIDHandler extends HWIDHandler {
    private MySQLSourceConfig mySQLHolder;
    private String query;
    private String banMessage;
    private String isBannedName;
    private String loginName;
    private String hwidName;
    private String[] queryParams;
    private String queryUpd;
    private String[] queryParamsUpd;
    private String queryBan;
    private String[] queryParamsBan;
    private String querySelect;
    private String[] queryParamsSelect;

    @Override
    public void check0(HWID hwid, String username) throws HWIDException {
        try {
            Connection c = mySQLHolder.getConnection();

            PreparedStatement s = c.prepareStatement(query);
            String[] replaceParams = {"hwid", String.valueOf(hwid.getSerializeString()), "login", username};
            for (int i = 0; i < queryParams.length; i++) {
                s.setString(i + 1, CommonHelper.replace(queryParams[i], replaceParams));
            }

            // Execute SQL query
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            try (ResultSet set = s.executeQuery()) {
                boolean isOne = false;
                boolean needWrite = true;
                while (set.next()) {
                    isOne = true;
                    boolean isBanned = set.getBoolean(isBannedName);
                    if (isBanned) throw new HWIDException(banMessage);
                    String login = set.getString(loginName);
                    if (username.equals(login)) {
                        needWrite = false;
                    }
                }
                if (!isOne) {
                    writeHWID(hwid, username, c);
                    return;
                }
                if (needWrite) {
                    writeHWID(hwid, username, c);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void writeHWID(HWID hwid, String username, Connection c) {
        LogHelper.debug("Write HWID %s from username %s", hwid.toString(), username);
        try (PreparedStatement a = c.prepareStatement(queryUpd)) {
            //IF
            String[] replaceParamsUpd = {"hwid", String.valueOf(hwid.getSerializeString()), "login", username};
            for (int i = 0; i < queryParamsUpd.length; i++) {
                a.setString(i + 1, CommonHelper.replace(queryParamsUpd[i], replaceParamsUpd));
            }
            a.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            a.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setIsBanned(HWID hwid, boolean isBanned) {
        LogHelper.debug("%s Request HWID: %s", isBanned ? "Ban" : "UnBan", hwid.toString());
        Connection c = null;
        try {
            c = mySQLHolder.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try (PreparedStatement a = c.prepareStatement(queryBan)) {
            //IF
            String[] replaceParamsUpd = {"hwid", String.valueOf(hwid.getSerializeString()), "isBanned", isBanned ? "1" : "0"};
            for (int i = 0; i < queryParamsBan.length; i++) {
                a.setString(i + 1, CommonHelper.replace(queryParamsBan[i], replaceParamsUpd));
            }
            a.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            a.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
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
        try {
            LogHelper.debug("Try find HWID from username %s", username);
            Connection c = mySQLHolder.getConnection();
            PreparedStatement s = c.prepareStatement(querySelect);
            String[] replaceParams = {"login", username};
            for (int i = 0; i < queryParamsSelect.length; i++) {
                s.setString(i + 1, CommonHelper.replace(queryParamsSelect[i], replaceParams));
            }
            String hwid_str;
            try (ResultSet set = s.executeQuery()) {
                if (!set.next()) {
                    LogHelper.error(new HWIDException("HWID not found"));
                    return new ArrayList<>();
                }
                hwid_str = set.getString(hwidName);
            }
            ArrayList<HWID> list = new ArrayList<>();
            HWID hwid = OshiHWID.gson.fromJson(hwid_str, OshiHWID.class);
            if (hwid.isNull()) {
                LogHelper.warning("Null HWID");
            } else {
                list.add(hwid);
                LogHelper.debug("Username: %s HWID: %s", username, hwid.toString());
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void close() {
        // Do nothing
    }
}
