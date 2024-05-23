package pro.gravit.launchserver.auth.core.openid;

import pro.gravit.launchserver.auth.SQLSourceConfig;
import pro.gravit.utils.helper.LogHelper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.UUID;

public class SQLServerSessionStore implements ServerSessionStore {
    private static final String CREATE_TABLE = """
            create table if not exists `gravit_server_session` (
              id int auto_increment,
              uuid varchar(36),
              username varchar(255),
              server_id varchar(41),
              primary key (id),
              unique (uuid),
              unique (username)
            );
            """;
    private static final String DELETE_SERVER_ID = """
            delete from `gravit_server_session` where uuid = ?
            """;
    private static final String INSERT_SERVER_ID = """
            insert into `gravit_server_session` (uuid, username, server_id) values (?, ?, ?)
            """;
    private static final String SELECT_SERVER_ID_BY_USERNAME = """
            select server_id from `gravit_server_session` where username = ?
            """;

    private final SQLSourceConfig sqlSourceConfig;

    public SQLServerSessionStore(SQLSourceConfig sqlSourceConfig) {
        this.sqlSourceConfig = sqlSourceConfig;
    }

    @Override
    public boolean joinServer(UUID uuid, String username, String serverId) {
        try (var connection = sqlSourceConfig.getConnection()) {
            connection.setAutoCommit(false);
            var savepoint = connection.setSavepoint();
            try (var deleteServerIdStmt = connection.prepareStatement(DELETE_SERVER_ID);
                 var insertServerIdStmt = connection.prepareStatement(INSERT_SERVER_ID)) {
                deleteServerIdStmt.setString(1, uuid.toString());
                deleteServerIdStmt.execute();
                insertServerIdStmt.setString(1, uuid.toString());
                insertServerIdStmt.setString(2, username);
                insertServerIdStmt.setString(3, serverId);
                insertServerIdStmt.execute();
                connection.commit();
                return true;
            } catch (Exception e) {
                connection.rollback(savepoint);
                throw e;
            }
        } catch (SQLException e) {
            LogHelper.debug("Can't join server. Username: %s".formatted(username));
            LogHelper.error(e);
        }

        return false;
    }

    @Override
    public String getServerIdByUsername(String username) {
        try (var connection = sqlSourceConfig.getConnection();
             var selectServerId = connection.prepareStatement(SELECT_SERVER_ID_BY_USERNAME)) {
            selectServerId.setString(1, username);
            try (var rs = selectServerId.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getString("server_id");
            }
        } catch (SQLException e) {
            LogHelper.debug("Can't find server id by username. Username: %s".formatted(username));
            LogHelper.error(e);
        }
        return null;
    }

    public void init() {
        try (var connection = sqlSourceConfig.getConnection()) {
            connection.setAutoCommit(false);
            var savepoint = connection.setSavepoint();
            try (var createTableStmt = connection.prepareStatement(CREATE_TABLE)) {
                createTableStmt.execute();
                connection.commit();
            } catch (Exception e) {
                connection.rollback(savepoint);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
