package pro.gravit.launchserver.auth.hwid;

import pro.gravit.launcher.hwid.HWID;
import pro.gravit.launchserver.Reconfigurable;
import pro.gravit.utils.ProviderMap;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.LogHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class HWIDHandler implements AutoCloseable, Reconfigurable {
    public static final ProviderMap<HWIDHandler> providers = new ProviderMap<>("HWIDHandler");
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

    @Override
    public Map<String, Command> getCommands() {
        Map<String, Command> commands = new HashMap<>();
        commands.put("ban", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                List<HWID> target = getHwid(args[0]);
                ban(target);
            }
        });
        commands.put("unban", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                List<HWID> target = getHwid(args[0]);
                unban(target);
            }
        });
        commands.put("gethwid", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                List<HWID> target = getHwid(args[0]);
                for (HWID hwid : target) {
                    if (hwid == null) {
                        LogHelper.error("[%s] HWID: null", args[0]);
                        continue;
                    }
                    LogHelper.info("[%s] HWID: %s", args[0], hwid.toString());
                }
            }
        });
        return commands;
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
