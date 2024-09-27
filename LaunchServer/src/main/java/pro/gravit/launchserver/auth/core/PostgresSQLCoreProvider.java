package pro.gravit.launchserver.auth.core;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.PostgreSQLSourceConfig;
import pro.gravit.launchserver.auth.SQLSourceConfig;

public class PostgresSQLCoreProvider extends AbstractSQLCoreProvider {
    public PostgreSQLSourceConfig postgresSQLHolder;

    @Override
    public SQLSourceConfig getSQLConfig() {
        return postgresSQLHolder;
    }

    @Override
    public void init(LaunchServer server, AuthProviderPair pair) {
        super.init(server, pair);
        logger.warn("Method 'postgresql' deprecated and may be removed in future release. Please use new 'sql' method: https://gravitlauncher.com/auth");
    }
}
