package ru.gravit.launchserver.manangers;

import ru.gravit.launchserver.Reconfigurable;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.util.HashMap;
import java.util.Objects;

public class ReconfigurableManager {
    private final HashMap<String, Reconfigurable> RECONFIGURABLE = new HashMap<>();

    public void registerReconfigurable(String name, Reconfigurable reconfigurable) {
        VerifyHelper.putIfAbsent(RECONFIGURABLE, name.toLowerCase(), Objects.requireNonNull(reconfigurable, "adapter"),
                String.format("Reloadable has been already registered: '%s'", name));
    }
    public Reconfigurable unregisterReconfigurable(String name)
    {
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
