package pro.gravit.launchserver.auth.handler;

import org.postgresql.util.PGobject;
import pro.gravit.launchserver.auth.PostgreSQLSourceConfig;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class PostgreSQLAuthHandler extends CachedAuthHandler {
    private PostgreSQLSourceConfig postgreSQLHolder;
    private String uuidColumn;
    private String usernameColumn;
    private String accessTokenColumn;
    private String serverIDColumn;


    private String queryByUUIDSQL;
    private String queryByUsernameSQL;
    private String updateAuthSQL;
    private String updateServerIDSQL;

    @Override
    public void close() {
        postgreSQLHolder.close();
    }

    private Entry constructEntry(ResultSet set) throws SQLException {
        return set.next() ? new Entry(UUID.fromString(set.getString(uuidColumn)),
                set.getString(usernameColumn), set.getString(accessTokenColumn), set.getString(serverIDColumn)) : null;
    }

    @Override
    protected Entry fetchEntry(String username) throws IOException {
        return query(queryByUsernameSQL, username);
    }

    @Override
    protected Entry fetchEntry(UUID uuid) throws IOException {
        return query(queryByUUIDSQL, uuid);
    }

    @Override
    protected boolean updateAuth(UUID uuid, String username, String accessToken) throws IOException {
        try (Connection c = postgreSQLHolder.getConnection();
             PreparedStatement s = c.prepareStatement(updateAuthSQL)) {
            s.setString(1, username); // Username case
            s.setString(2, accessToken);

            PGobject uuidObject = new PGobject();
            uuidObject.setType("uuid");
            uuidObject.setValue(uuid.toString());
            s.setObject(3, uuidObject);

            // Execute update
            s.setQueryTimeout(PostgreSQLSourceConfig.TIMEOUT);
            return s.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected boolean updateServerID(UUID uuid, String serverID) throws IOException {
        try (Connection c = postgreSQLHolder.getConnection();
             PreparedStatement s = c.prepareStatement(updateServerIDSQL)) {
            s.setString(1, serverID);

            PGobject uuidObject = new PGobject();
            uuidObject.setType("uuid");
            uuidObject.setValue(uuid.toString());
            s.setObject(2, uuidObject);

            // Execute update
            s.setQueryTimeout(PostgreSQLSourceConfig.TIMEOUT);
            return s.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    private Entry query(String sql, String value) throws IOException {
        try (Connection c = postgreSQLHolder.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, value);

            // Execute query
            s.setQueryTimeout(PostgreSQLSourceConfig.TIMEOUT);
            try (ResultSet set = s.executeQuery()) {
                return constructEntry(set);
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    private Entry query(String sql, UUID value) throws IOException {
        try (Connection c = postgreSQLHolder.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            PGobject uuidObject = new PGobject();
            uuidObject.setType("uuid");
            uuidObject.setValue(value.toString());

            s.setObject(1, uuidObject);

            // Execute query
            s.setQueryTimeout(PostgreSQLSourceConfig.TIMEOUT);
            try (ResultSet set = s.executeQuery()) {
                return constructEntry(set);
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}