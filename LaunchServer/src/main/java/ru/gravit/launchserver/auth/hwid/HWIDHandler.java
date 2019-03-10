package ru.gravit.launchserver.auth.hwid;

import ru.gravit.launcher.HWID;
import ru.gravit.utils.helper.VerifyHelper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public abstract class HWIDHandler implements AutoCloseable {
    private static final Map<String, Class<? extends HWIDHandler>> HW_HANDLERS = new ConcurrentHashMap<>(4);
    private static boolean registredHandl = false;


    public static void registerHandler(String name, Class<? extends HWIDHandler> adapter) {
        VerifyHelper.verifyIDName(name);
        VerifyHelper.putIfAbsent(HW_HANDLERS, name, Objects.requireNonNull(adapter, "adapter"),
                String.format("HWID handler has been already registered: '%s'", name));
    }

    public static void registerHandlers() {
        if (!registredHandl) {
            registerHandler("accept", AcceptHWIDHandler.class);
            registerHandler("mysql", MysqlHWIDHandler.class);
            registerHandler("json", JsonHWIDHandler.class);
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

    public abstract List<HWID> getHwid(String username) throws HWIDException;

    public abstract void unban(List<HWID> hwid) throws HWIDException;

    public static Class<? extends HWIDHandler> getHandlerClass(String name) {
        return HW_HANDLERS.get(name);
    }

    public static String getHandlerName(Class<? extends HWIDHandler> clazz) {
        for (Map.Entry<String, Class<? extends HWIDHandler>> e : HW_HANDLERS.entrySet()) {
            if (e.getValue().equals(clazz)) return e.getKey();
        }
        return null;
    }
}
