package pro.gravit.launchserver.auth.core;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.HikariSQLSourceConfig;
import pro.gravit.launchserver.auth.SQLSourceConfig;

public class SQLCoreProvider extends AbstractSQLCoreProvider {
    public HikariSQLSourceConfig holder;

    @Override
    public void close() {
        super.close();
        holder.close();
    }

    @Override
    public void init(LaunchServer server, AuthProviderPair pair) {
        holder.init();
        super.init(server, pair);
    }

    @Override
    public SQLSourceConfig getSQLConfig() {
        return holder;
    }
}
