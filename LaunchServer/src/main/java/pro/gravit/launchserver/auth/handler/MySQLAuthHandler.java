package pro.gravit.launchserver.auth.handler;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.MySQLSourceConfig;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class MySQLAuthHandler extends CachedAuthHandler {
    private MySQLSourceConfig mySQLHolder;
    private String uuidColumn;
    private String usernameColumn;
    private String accessTokenColumn;
    private String serverIDColumn;
    private String table;

    // Prepared SQL queries
    private transient String queryByUUIDSQL;
    private transient String queryByUsernameSQL;
    private transient String updateAuthSQL;
    private transient String updateServerIDSQL;

    @Override
    public void init(LaunchServer srv) {
        super.init(srv);
        //Verify
        if (mySQLHolder == null) LogHelper.error("[Verify][AuthHandler] mySQLHolder cannot be null");
        if (uuidColumn == null) LogHelper.error("[Verify][AuthHandler] uuidColumn cannot be null");
        if (usernameColumn == null) LogHelper.error("[Verify][AuthHandler] usernameColumn cannot be null");
        if (accessTokenColumn == null) LogHelper.error("[Verify][AuthHandler] accessTokenColumn cannot be null");
        if (serverIDColumn == null) LogHelper.error("[Verify][AuthHandler] serverIDColumn cannot be null");
        if (table == null) LogHelper.error("[Verify][AuthHandler] table cannot be null");
        // Prepare SQL queries
        queryByUUIDSQL = String.format("SELECT %s, %s, %s, %s FROM %s WHERE %s=? LIMIT 1",
                uuidColumn, usernameColumn, accessTokenColumn, serverIDColumn, table, uuidColumn);
        queryByUsernameSQL = String.format("SELECT %s, %s, %s, %s FROM %s WHERE %s=? LIMIT 1",
                uuidColumn, usernameColumn, accessTokenColumn, serverIDColumn, table, usernameColumn);
        updateAuthSQL = String.format("UPDATE %s SET %s=?, %s=?, %s=NULL WHERE %s=? LIMIT 1",
                table, usernameColumn, accessTokenColumn, serverIDColumn, uuidColumn);
        updateServerIDSQL = String.format("UPDATE %s SET %s=? WHERE %s=? LIMIT 1",
                table, serverIDColumn, uuidColumn);
    }

    @Override
    public void close() {
        mySQLHolder.close();
    }

    private Entry constructEntry(ResultSet set) throws SQLException {
        return set.next() ? new Entry(UUID.fromString(set.getString(uuidColumn)), set.getString(usernameColumn),
                set.getString(accessTokenColumn), set.getString(serverIDColumn)) : null;
    }

    @Override
    protected Entry fetchEntry(String username) throws IOException {
        return query(queryByUsernameSQL, username);
    }

    @Override
    protected Entry fetchEntry(UUID uuid) throws IOException {
        return query(queryByUUIDSQL, uuid.toString());
    }

    private Entry query(String sql, String value) throws IOException {
        try (Connection c = mySQLHolder.getConnection()) {
            PreparedStatement s = c.prepareStatement(sql);
            s.setString(1, value);
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            try (ResultSet set = s.executeQuery()) {
                return constructEntry(set);
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected boolean updateAuth(UUID uuid, String username, String accessToken) throws IOException {
        try (Connection c = mySQLHolder.getConnection()) {
            PreparedStatement s = c.prepareStatement(updateAuthSQL);
            s.setString(1, username); // Username case
            s.setString(2, accessToken);
            s.setString(3, uuid.toString());
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            return s.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected boolean updateServerID(UUID uuid, String serverID) throws IOException {
        try (Connection c = mySQLHolder.getConnection()) {
            PreparedStatement s = c.prepareStatement(updateServerIDSQL);
            s.setString(1, serverID);
            s.setString(2, uuid.toString());
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            return s.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
