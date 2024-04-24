package pro.gravit.launchserver.auth.core;

import pro.gravit.launchserver.auth.HikariSQLSourceConfig;
import pro.gravit.launchserver.auth.SQLSourceConfig;

public class SQLCoreProvider extends AbstractSQLCoreProvider {
    public HikariSQLSourceConfig holder;
    @Override
    public SQLSourceConfig getSQLConfig() {
        return holder;
    }
}
