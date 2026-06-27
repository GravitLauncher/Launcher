package pro.gravit.launchserver.auth;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.utils.helper.VerifyHelper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static java.util.concurrent.TimeUnit.MINUTES;

public final class MySQLSourceConfig implements AutoCloseable, SQLSourceConfig {

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
    private String address;
    private int port;
    private boolean useSSL;
    private boolean verifyCertificates;
    private String username;
    private String password;
    private String database;
    private String timezone;
    private final long hikariMaxLifetime = MINUTES.toMillis(30);
    private boolean useHikari;

    // Cache
    private transient volatile DataSource source;
    private transient boolean hikari;


    public MySQLSourceConfig(String poolName) {
        this.poolName = poolName;
    }

    public MySQLSourceConfig(String poolName, String address, int port, String username, String password, String database) {
        this.poolName = poolName;
        this.address = address;
        this.port = port;
        this.username = username;
        this.password = password;
        this.database = database;
    }

    public MySQLSourceConfig(String poolName, DataSource source, boolean hikari) {
        this.poolName = poolName;
        this.source = source;
        this.hikari = hikari;
    }

    @Override
    public synchronized void close() {
        DataSource ds = source;
        source = null;
        if (hikari && ds instanceof HikariDataSource hikariDataSource && !hikariDataSource.isClosed()) {
            hikariDataSource.close();
        }
        hikari = false;
    }


    public Connection getConnection() throws SQLException {
        DataSource ds = source;
        if (ds == null) {
            synchronized (this) {
                ds = source;
                if (ds == null) {
                    ds = initDataSource();
                    source = ds;
                }
            }
        }
        return ds.getConnection();
    }

    private DataSource initDataSource() throws SQLException {
        MysqlDataSource mysqlSource = new MysqlDataSource();
        mysqlSource.setCharacterEncoding("UTF-8");

        mysqlSource.setPrepStmtCacheSize(250);
        mysqlSource.setPrepStmtCacheSqlLimit(2048);
        mysqlSource.setCachePrepStmts(true);
        mysqlSource.setUseServerPrepStmts(true);

        mysqlSource.setCacheServerConfiguration(true);
        mysqlSource.setUseLocalSessionState(true);
        mysqlSource.setRewriteBatchedStatements(true);
        mysqlSource.setMaintainTimeStats(false);
        mysqlSource.setUseUnbufferedInput(false);
        mysqlSource.setUseReadAheadInput(false);
        mysqlSource.setUseSSL(useSSL);
        mysqlSource.setVerifyServerCertificate(verifyCertificates);
        mysqlSource.setServerName(address);
        mysqlSource.setPortNumber(port);
        mysqlSource.setUser(username);
        mysqlSource.setPassword(password);
        mysqlSource.setDatabaseName(database);
        mysqlSource.setTcpNoDelay(true);
        if (timezone != null) mysqlSource.setServerTimezone(timezone);
        hikari = false;
        DataSource result = mysqlSource;
        if (useHikari) {
            try {
                Class.forName("com.zaxxer.hikari.HikariDataSource");
                hikari = true;
                HikariConfig hikariConfig = new HikariConfig();
                hikariConfig.setDataSource(mysqlSource);
                hikariConfig.setPoolName(poolName);
                hikariConfig.setMinimumIdle(1);
                hikariConfig.setMaximumPoolSize(MAX_POOL_SIZE);
                hikariConfig.setConnectionTestQuery("SELECT 1");
                hikariConfig.setConnectionTimeout(1000);
                hikariConfig.setLeakDetectionThreshold(2000);
                hikariConfig.setMaxLifetime(hikariMaxLifetime);
                result = new HikariDataSource(hikariConfig);
            } catch (ClassNotFoundException ignored) {
                logger.debug("HikariCP isn't in classpath for '{}'", poolName);
            }
        }
        return result;
    }
}
