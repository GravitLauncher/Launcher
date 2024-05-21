package pro.gravit.launchserver.auth;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.Consumer;

public class HikariSQLSourceConfig implements SQLSourceConfig {
    private transient HikariDataSource dataSource;
    private String dsClass;
    private Properties dsProps;
    private String driverClass;
    private String jdbcUrl;
    private String username;
    private String password;

    public void init() {
        if (dataSource != null) {
            return;
        }
        HikariConfig config = new HikariConfig();
        consumeIfNotNull(config::setDataSourceClassName, dsClass);
        consumeIfNotNull(config::setDataSourceProperties, dsProps);
        consumeIfNotNull(config::setDriverClassName, driverClass);
        consumeIfNotNull(config::setJdbcUrl, jdbcUrl);
        consumeIfNotNull(config::setUsername, username);
        consumeIfNotNull(config::setPassword, password);

        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        dataSource.close();
    }

    private static <T> void consumeIfNotNull(Consumer<T> consumer, T val) {
        if (val != null) {
            consumer.accept(val);
        }
    }
}
