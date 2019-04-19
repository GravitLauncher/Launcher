package ru.gravit.launchserver.auth.hwid;

import ru.gravit.launcher.HWID;
import ru.gravit.utils.ProviderMap;

import java.util.List;

public abstract class HWIDHandler implements AutoCloseable {
    public static ProviderMap<HWIDHandler> providers = new ProviderMap<>();
    private static boolean registredHandl = false;


    public static void registerHandlers() {
        if (!registredHandl) {
            providers.registerProvider("accept", AcceptHWIDHandler.class);
            providers.registerProvider("mysql", MysqlHWIDHandler.class);
            providers.registerProvider("json", JsonHWIDHandler.class);
            providers.registerProvider("memory", MemoryHWIDHandler.class);
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
