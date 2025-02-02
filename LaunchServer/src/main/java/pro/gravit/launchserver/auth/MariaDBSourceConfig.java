package pro.gravit.launchserver.auth;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mariadb.jdbc.MariaDbDataSource;
import pro.gravit.utils.helper.VerifyHelper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static java.util.concurrent.TimeUnit.MINUTES;

public final class MariaDBSourceConfig implements AutoCloseable, SQLSourceConfig {

    public static final int TIMEOUT = VerifyHelper.verifyInt(
            Integer.parseUnsignedInt(System.getProperty("launcher.mysql.idleTimeout", Integer.toString(5000))),
            VerifyHelper.POSITIVE, "launcher.mysql.idleTimeout can't be <= 5000");
    private static final int MAX_POOL_SIZE = VerifyHelper.verifyInt(
            Integer.parseUnsignedInt(System.getProperty("launcher.mysql.maxPoolSize", Integer.toString(3))),
            VerifyHelper.POSITIVE, "launcher.mysql.maxPoolSize can't be <= 0");

    // Instance
    private transient final String poolName;
    private transient final Logger logger = LogManager.getLogger();

    // Config
    private String url;
    private String username;
    private String password;
    private long hikariMaxLifetime = MINUTES.toMillis(30);
    private boolean useHikari;

    // Cache
    private transient DataSource source;
    private transient boolean hikari;


    public MariaDBSourceConfig(String poolName) {
        this.poolName = poolName;
    }

    public MariaDBSourceConfig(String poolName, String url, String username, String password) {
        this.poolName = poolName;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public MariaDBSourceConfig(String poolName, DataSource source, boolean hikari) {
        this.poolName = poolName;
        this.source = source;
        this.hikari = hikari;
    }

    @Override
    public synchronized void close() {
        if (hikari)
            ((HikariDataSource) source).close();
    }


    public synchronized Connection getConnection() throws SQLException {
        if (source == null) { // New data source
            MariaDbDataSource mariaDbDataSource = new MariaDbDataSource();
            mariaDbDataSource.setUser(username);
            mariaDbDataSource.setPassword(password);
            mariaDbDataSource.setUrl(url);
            hikari = false;
            // Try using HikariCP
            source = mariaDbDataSource;
            if (useHikari) {
                try {
                    Class.forName("com.zaxxer.hikari.HikariDataSource");
                    hikari = true; // Used for shutdown. Not instanceof because of possible classpath error
                    HikariConfig hikariConfig = new HikariConfig();
                    hikariConfig.setDataSource(mariaDbDataSource);
                    hikariConfig.setPoolName(poolName);
                    hikariConfig.setMinimumIdle(1);
                    hikariConfig.setMaximumPoolSize(MAX_POOL_SIZE);
                    hikariConfig.setConnectionTestQuery("SELECT 1");
                    hikariConfig.setConnectionTimeout(1000);
                    hikariConfig.setLeakDetectionThreshold(2000);
                    hikariConfig.setMaxLifetime(hikariMaxLifetime);
                    // Set HikariCP pool
                    // Replace source with hds
                    source = new HikariDataSource(hikariConfig);
                } catch (ClassNotFoundException ignored) {
                    logger.debug("HikariCP isn't in classpath for '{}'", poolName);
                }
            }

        }
        return source.getConnection();
    }
}
