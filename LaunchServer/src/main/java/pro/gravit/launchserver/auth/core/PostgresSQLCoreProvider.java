package pro.gravit.launchserver.auth.core;

import pro.gravit.launchserver.auth.PostgreSQLSourceConfig;
import pro.gravit.launchserver.auth.SQLSourceConfig;

public class PostgresSQLCoreProvider extends AbstractSQLCoreProvider {
    public PostgreSQLSourceConfig postgresSQLHolder;

    @Override
    public SQLSourceConfig getSQLConfig() {
        return postgresSQLHolder;
    }
}
