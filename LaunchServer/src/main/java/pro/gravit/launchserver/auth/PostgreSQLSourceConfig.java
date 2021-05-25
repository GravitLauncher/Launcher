package pro.gravit.launchserver.auth;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.ds.PGSimpleDataSource;
import pro.gravit.utils.helper.VerifyHelper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class PostgreSQLSourceConfig implements AutoCloseable {
    public static final int TIMEOUT = VerifyHelper.verifyInt(
            Integer.parseUnsignedInt(System.getProperty("launcher.postgresql.idleTimeout", Integer.toString(5000))),
            VerifyHelper.POSITIVE, "launcher.postgresql.idleTimeout can't be <= 5000");
    private static final int MAX_POOL_SIZE = VerifyHelper.verifyInt(
            Integer.parseUnsignedInt(System.getProperty("launcher.postgresql.maxPoolSize", Integer.toString(3))),
            VerifyHelper.POSITIVE, "launcher.postgresql.maxPoolSize can't be <= 0");
    private transient final Logger logger = LogManager.getLogger();
    // Instance
    private String poolName;
    // Config
    private String[] addresses;
    private int[] ports;
    private String username;
    private String password;
    private String database;

    // Cache
    private DataSource source;
    private boolean hikari;

    @Override
    public synchronized void close() {
        if (hikari) { // Shutdown hikari pool
            ((HikariDataSource) source).close();
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        if (source == null) { // New data source
            PGSimpleDataSource postgresqlSource = new PGSimpleDataSource();

            // Set credentials
            postgresqlSource.setServerNames(addresses);
            postgresqlSource.setPortNumbers(ports);
            postgresqlSource.setUser(username);
            postgresqlSource.setPassword(password);
            postgresqlSource.setDatabaseName(database);

            // Try using HikariCP
            source = postgresqlSource;

            //noinspection Duplicates
            try {
                Class.forName("com.zaxxer.hikari.HikariDataSource");
                hikari = true; // Used for shutdown. Not instanceof because of possible classpath error

                // Set HikariCP pool
                HikariDataSource hikariSource = new HikariDataSource();
                hikariSource.setDataSource(source);

                // Set pool settings
                hikariSource.setPoolName(poolName);
                hikariSource.setMinimumIdle(0);
                hikariSource.setMaximumPoolSize(MAX_POOL_SIZE);
                hikariSource.setIdleTimeout(TIMEOUT * 1000L);

                // Replace source with hds
                source = hikariSource;
                logger.info("HikariCP pooling enabled for '{}'", poolName);
            } catch (ClassNotFoundException ignored) {
                logger.warn("HikariCP isn't in classpath for '{}'", poolName);
            }
        }
        return source.getConnection();
    }
}