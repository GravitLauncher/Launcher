package ru.gravit.launchserver.auth;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ru.gravit.launcher.serialize.config.ConfigObject;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launcher.serialize.config.entry.BooleanConfigEntry;
import ru.gravit.launcher.serialize.config.entry.IntegerConfigEntry;
import ru.gravit.launcher.serialize.config.entry.StringConfigEntry;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.VerifyHelper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class MySQLSourceConfig extends ConfigObject implements AutoCloseable {

    public static final int TIMEOUT = VerifyHelper.verifyInt(
            Integer.parseUnsignedInt(System.getProperty("launcher.mysql.idleTimeout", Integer.toString(5000))),
            VerifyHelper.POSITIVE, "launcher.mysql.idleTimeout can't be <= 5000");
    private static final int MAX_POOL_SIZE = VerifyHelper.verifyInt(
            Integer.parseUnsignedInt(System.getProperty("launcher.mysql.maxPoolSize", Integer.toString(3))),
            VerifyHelper.POSITIVE, "launcher.mysql.maxPoolSize can't be <= 0");

    // Instance
    private final String poolName;

    // Config
    private final String address;
    private final int port;
    private final boolean useSSL;
    private final boolean verifyCertificates;
    private final String username;
    private final String password;
    private final String database;
    private String timeZone;

    // Cache
    private DataSource source;
    private boolean hikari;


    public MySQLSourceConfig(String poolName, BlockConfigEntry block) {
        super(block);
        this.poolName = poolName;
        address = VerifyHelper.verify(block.getEntryValue("address", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "MySQL address can't be empty");
        port = VerifyHelper.verifyInt(block.getEntryValue("port", IntegerConfigEntry.class),
                VerifyHelper.range(0, 65535), "Illegal MySQL port");
        username = VerifyHelper.verify(block.getEntryValue("username", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "MySQL username can't be empty");
        password = block.getEntryValue("password", StringConfigEntry.class);
        database = VerifyHelper.verify(block.getEntryValue("database", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "MySQL database can't be empty");
        timeZone = block.hasEntry("timezone") ? VerifyHelper.verify(block.getEntryValue("timezone", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "MySQL time zone can't be empty") : null;
        // Password shouldn't be verified
        useSSL = block.hasEntry("useSSL") ? block.getEntryValue("useSSL", BooleanConfigEntry.class) : true;
        verifyCertificates = block.hasEntry("verifyCertificates") ? block.getEntryValue("verifyCertificates", BooleanConfigEntry.class) : false;
    }

    @Override
    public synchronized void close() {
        if (hikari)
            ((HikariDataSource) source).close();
    }


    public synchronized Connection getConnection() throws SQLException {
        if (source == null) { // New data source
            MysqlDataSource mysqlSource = new MysqlDataSource();
            mysqlSource.setCharacterEncoding("UTF-8");

            // Prep statements cache
            mysqlSource.setPrepStmtCacheSize(250);
            mysqlSource.setPrepStmtCacheSqlLimit(2048);
            mysqlSource.setCachePrepStmts(true);
            mysqlSource.setUseServerPrepStmts(true);

            // General optimizations
            mysqlSource.setCacheServerConfiguration(true);
            mysqlSource.setUseLocalSessionState(true);
            mysqlSource.setRewriteBatchedStatements(true);
            mysqlSource.setMaintainTimeStats(false);
            mysqlSource.setUseUnbufferedInput(false);
            mysqlSource.setUseReadAheadInput(false);
            mysqlSource.setUseSSL(useSSL);
            mysqlSource.setVerifyServerCertificate(verifyCertificates);
            // Set credentials
            mysqlSource.setServerName(address);
            mysqlSource.setPortNumber(port);
            mysqlSource.setUser(username);
            mysqlSource.setPassword(password);
            mysqlSource.setDatabaseName(database);
            mysqlSource.setTcpNoDelay(true);
            if (timeZone != null) mysqlSource.setServerTimezone(timeZone);
            hikari = false;
            // Try using HikariCP
            source = mysqlSource;
            try {
                Class.forName("com.zaxxer.hikari.HikariDataSource");
                hikari = true; // Used for shutdown. Not instanceof because of possible classpath error
                HikariConfig cfg = new HikariConfig();
                cfg.setDataSource(mysqlSource);
                cfg.setPoolName(poolName);
                cfg.setMaximumPoolSize(MAX_POOL_SIZE);
                // Set HikariCP pool
                // Replace source with hds
                source = new HikariDataSource(cfg);
                LogHelper.info("HikariCP pooling enabled for '%s'", poolName);
            } catch (ClassNotFoundException ignored) {
                LogHelper.warning("HikariCP isn't in classpath for '%s'", poolName);
            }
        }
        return source.getConnection();
    }
}
