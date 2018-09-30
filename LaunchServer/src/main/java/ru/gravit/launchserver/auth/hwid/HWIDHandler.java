package ru.gravit.launchserver.auth.hwid;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launcher.serialize.config.ConfigObject;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;

public abstract class HWIDHandler extends ConfigObject implements AutoCloseable {
    private static final Map<String, Adapter<HWIDHandler>> HW_HANDLERS = new ConcurrentHashMap<>(4);
    public static final HWID nullHWID = HWID.gen(0, 0, 0);
    private static boolean registredHandl = false;

    @LauncherAPI
    public static HWIDHandler newHandler(String name, BlockConfigEntry block) {
        Adapter<HWIDHandler> authHandlerAdapter = VerifyHelper.getMapValue(HW_HANDLERS, name,
                String.format("Unknown HWID handler: '%s'", name));
        return authHandlerAdapter.convert(block);
    }

    @LauncherAPI
    public static void registerHandler(String name, Adapter<HWIDHandler> adapter) {
        VerifyHelper.verifyIDName(name);
        VerifyHelper.putIfAbsent(HW_HANDLERS, name, Objects.requireNonNull(adapter, "adapter"),
                String.format("HWID handler has been already registered: '%s'", name));
    }

    public static void registerHandlers() {
        if (!registredHandl) {
            registerHandler("accept", AcceptHWIDHandler::new);
            registerHandler("mysql", MysqlHWIDHandler::new);
            registerHandler("json", JsonHWIDHandler::new);
            registredHandl = true;
        }
    }

    protected HWIDHandler(BlockConfigEntry block) {
        super(block);
    }

    public abstract void ban(List<HWID> hwid) throws HWIDException;

    public void check(HWID hwid, String username) throws HWIDException {
        if (nullHWID.equals(hwid)) return;
        check0(hwid, username);
    }

    public abstract void check0(HWID hwid, String username) throws HWIDException;

    @Override
    public abstract void close();

    public abstract List<HWID> getHwid(String username) throws HWIDException;

    public abstract void unban(List<HWID> hwid) throws HWIDException;
}
