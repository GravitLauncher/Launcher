package pro.gravit.launchserver.auth.hwid;

import java.util.List;

import pro.gravit.launcher.HWID;
import pro.gravit.utils.ProviderMap;

public abstract class HWIDHandler implements AutoCloseable {
    public static ProviderMap<HWIDHandler> providers = new ProviderMap<>("HWIDHandler");
    private static boolean registredHandl = false;


    public static void registerHandlers() {
        if (!registredHandl) {
            providers.register("accept", AcceptHWIDHandler.class);
            providers.register("mysql", MysqlHWIDHandler.class);
            providers.register("json", JsonHWIDHandler.class);
            providers.register("jsonfile", JsonFileHWIDHandler.class);
            providers.register("memory", MemoryHWIDHandler.class);
            registredHandl = true;
        }
    }

    public abstract void ban(List<HWID> hwid) throws HWIDException;

    public void check(HWID hwid, String username) throws HWIDException {
        if (hwid.isNull()) return;
        check0(hwid, username);
    }

    public abstract void check0(HWID hwid, String username) throws HWIDException;

    @Override
    public abstract void close() throws Exception;

    public abstract void init();

    public abstract List<HWID> getHwid(String username) throws HWIDException;

    public abstract void unban(List<HWID> hwid) throws HWIDException;
}
