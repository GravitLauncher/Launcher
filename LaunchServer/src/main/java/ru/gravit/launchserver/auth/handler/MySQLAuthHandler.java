package ru.gravit.launchserver.auth.handler;

import ru.gravit.launchserver.auth.MySQLSourceConfig;

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

    // Prepared SQL queries
    private String queryByUUIDSQL;
    private String queryByUsernameSQL;
    private String updateAuthSQL;
    private String updateServerIDSQL;

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
        try {
            Connection c = mySQLHolder.getConnection();
            PreparedStatement s = c.prepareStatement(sql);
            s.setString(1, value);

            // Execute query
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
        try {
            Connection c = mySQLHolder.getConnection();
            PreparedStatement s = c.prepareStatement(updateAuthSQL);
            s.setString(1, username); // Username case
            s.setString(2, accessToken);
            s.setString(3, uuid.toString());

            // Execute update
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            return s.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected boolean updateServerID(UUID uuid, String serverID) throws IOException {
        try {
            Connection c = mySQLHolder.getConnection();
            PreparedStatement s = c.prepareStatement(updateServerIDSQL);
            s.setString(1, serverID);
            s.setString(2, uuid.toString());

            // Execute update
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            return s.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
