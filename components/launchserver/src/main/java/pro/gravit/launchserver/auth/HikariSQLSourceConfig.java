package pro.gravit.launchserver.auth;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class HikariSQLSourceConfig implements SQLSourceConfig {
    private static final Logger logger = LogManager.getLogger(HikariSQLSourceConfig.class);

    // — config fields (serialized) —
    private String dsClass;
    private Properties dsProps;
    private String driverClass;
    private String jdbcUrl;
    private String username;
    private String password;
    /**
     * When {@code true} the pool is created eagerly inside {@link #init()};
     * otherwise the first {@link #getConnection()} call triggers initialisation.
     */
    private boolean initializeAtStart;

    // — runtime state (never serialized) —
    private transient volatile HikariDataSource dataSource;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /** Called once after deserialization. Safe to call multiple times. */
    public synchronized void init() {
        if (initializeAtStart) {
            initPool();
        }
    }

    /** Closes the underlying connection pool if it has been opened. */
    @Override
    public synchronized void close() {
        HikariDataSource ds = this.dataSource;
        dataSource = null;
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }

    // -------------------------------------------------------------------------
    // SQLSourceConfig
    // -------------------------------------------------------------------------

    @Override
    public Connection getConnection() throws SQLException {
        HikariDataSource ds = dataSource;
        if (ds == null || ds.isClosed()) {
            // Double-checked locking – pool is created at most once.
            synchronized (this) {
                ds = dataSource;
                if (ds == null || ds.isClosed()) {
                    initPool();
                    ds = dataSource;
                }
            }
        }
        return ds.getConnection();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Creates the {@link HikariDataSource}. Must be called from a
     * {@code synchronized} context or before any concurrent access.
     */
    private void initPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            return; // already initialised
        }

        HikariConfig config = new HikariConfig();
        applyIfNonNull(config::setDataSourceClassName, dsClass);
        applyIfNonNull(config::setDataSourceProperties, dsProps);
        applyIfNonNull(config::setDriverClassName, driverClass);
        applyIfNonNull(config::setJdbcUrl, jdbcUrl);
        applyIfNonNull(config::setUsername, username);
        applyIfNonNull(config::setPassword, password);

        logger.debug("Initialising HikariCP connection pool (jdbcUrl={})", jdbcUrl);
        dataSource = new HikariDataSource(config);
    }

    /** Calls {@code setter} only when {@code value} is non-null. */
    private static <T> void applyIfNonNull(java.util.function.Consumer<T> setter, T value) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
