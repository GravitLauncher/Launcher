package ru.gravit.launchserver.manangers;

import ru.gravit.launchserver.Reconfigurable;
import ru.gravit.launchserver.Reloadable;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.util.HashMap;
import java.util.Objects;

public class ReconfigurableManager {
    private final HashMap<String, Reconfigurable> RECONFIGURABLE = new HashMap<>();
    public void registerReconfigurable(String name, Reconfigurable reconfigurable)
    {
        VerifyHelper.verifyIDName(name);
        VerifyHelper.putIfAbsent(RECONFIGURABLE, name, Objects.requireNonNull(reconfigurable, "adapter"),
                String.format("Reloadable has been already registered: '%s'", name));
    }
    public void printHelp(String name)
    {
        RECONFIGURABLE.get(name).printConfigHelp();
    }
    public void call(String name, String action, String[] args) throws Exception {
        RECONFIGURABLE.get(name).reconfig(action,args);
    }
    public void printReconfigurables()
    {
        LogHelper.info("Print reconfigurables");
        RECONFIGURABLE.forEach((k, v) -> LogHelper.subInfo(k));
        LogHelper.info("Found %d reconfigurables", RECONFIGURABLE.size());
    }
}
