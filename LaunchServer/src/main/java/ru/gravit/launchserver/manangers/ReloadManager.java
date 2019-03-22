package ru.gravit.launchserver.manangers;

import ru.gravit.launchserver.Reloadable;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.util.HashMap;
import java.util.Objects;

public class ReloadManager {
    private final HashMap<String, Reloadable> RELOADABLES = new HashMap<>();

    public void registerReloadable(String name, Reloadable reloadable) {
        VerifyHelper.putIfAbsent(RELOADABLES, name, Objects.requireNonNull(reloadable, "adapter"),
                String.format("Reloadable has been already registered: '%s'", name));
    }

    public void reloadAll() {
        RELOADABLES.forEach((k, v) -> {
            try {
                v.reload();
            } catch (Exception e) {
                LogHelper.error(e);
            }
        });
    }

    public void reload(String name) throws Exception {
        RELOADABLES.get(name).reload();
    }

    public void printReloadables() {
        LogHelper.info("Print reloadables");
        RELOADABLES.forEach((k, v) -> LogHelper.subInfo(k));
        LogHelper.info("Found %d reloadables", RELOADABLES.size());
    }
}
