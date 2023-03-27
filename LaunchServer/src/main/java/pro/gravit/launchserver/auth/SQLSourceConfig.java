package pro.gravit.launchserver.auth;

import java.sql.Connection;
import java.sql.SQLException;

public interface SQLSourceConfig {
    Connection getConnection() throws SQLException;

    void close();
}
