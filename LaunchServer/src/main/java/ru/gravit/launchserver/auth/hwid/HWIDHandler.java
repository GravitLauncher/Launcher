package ru.gravit.launchserver.auth.hwid;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import ru.gravit.launcher.HWID;
import ru.gravit.launcher.serialize.config.ConfigObject;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.utils.helper.VerifyHelper;

public abstract class HWIDHandler extends ConfigObject implements AutoCloseable {
    private static final Map<String, Adapter<HWIDHandler>> HW_HANDLERS = new ConcurrentHashMap<>(4);
    private static boolean registredHandl = false;


    public static HWIDHandler newHandler(String name, BlockConfigEntry block) {
        Adapter<HWIDHandler> authHandlerAdapter = VerifyHelper.getMapValue(HW_HANDLERS, name,
                String.format("Unknown HWID handler: '%s'", name));
        return authHandlerAdapter.convert(block);
    }


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
        if (hwid.isNull()) return;
        check0(hwid, username);
    }

    public abstract void check0(HWID hwid, String username) throws HWIDException;

    @Override
    public abstract void close();

    public abstract List<HWID> getHwid(String username) throws HWIDException;

    public abstract void unban(List<HWID> hwid) throws HWIDException;
}
