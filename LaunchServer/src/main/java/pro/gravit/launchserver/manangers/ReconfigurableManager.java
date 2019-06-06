package pro.gravit.launchserver.manangers;

import java.util.HashMap;
import java.util.Objects;

import pro.gravit.launchserver.Reconfigurable;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.VerifyHelper;

public class ReconfigurableManager {
    private final HashMap<String, Reconfigurable> RECONFIGURABLE = new HashMap<>();

    public void registerReconfigurable(String name, Reconfigurable reconfigurable) {
        VerifyHelper.putIfAbsent(RECONFIGURABLE, name.toLowerCase(), Objects.requireNonNull(reconfigurable, "adapter"),
                String.format("Reloadable has been already registered: '%s'", name));
    }

    public Reconfigurable unregisterReconfigurable(String name) {
        return RECONFIGURABLE.remove(name);
    }

    public void printHelp(String name) {
        RECONFIGURABLE.get(name.toLowerCase()).printConfigHelp();
    }

    public void call(String name, String action, String[] args) {
        RECONFIGURABLE.get(name.toLowerCase()).reconfig(action.toLowerCase(), args);
    }

    public void printReconfigurables() {
        LogHelper.info("Print reconfigurables");
        RECONFIGURABLE.forEach((k, v) -> LogHelper.subInfo(k));
        LogHelper.info("Found %d reconfigurables", RECONFIGURABLE.size());
    }
}
