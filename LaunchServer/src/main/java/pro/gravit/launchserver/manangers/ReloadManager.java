package pro.gravit.launchserver.manangers;

import pro.gravit.launchserver.Reloadable;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.VerifyHelper;

import java.util.HashMap;
import java.util.Objects;

public class ReloadManager {
    private final HashMap<String, Reloadable> RELOADABLES = new HashMap<>();

    public void registerReloadable(String name, Reloadable reloadable) {
        VerifyHelper.putIfAbsent(RELOADABLES, name.toLowerCase(), Objects.requireNonNull(reloadable, "adapter"),
                String.format("Reloadable has been already registered: '%s'", name.toLowerCase()));
    }

    public Reloadable unregisterReloadable(String name) {
        return RELOADABLES.remove(name);
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
        RELOADABLES.get(name.toLowerCase()).reload();
    }

    public void printReloadables() {
        LogHelper.info("Print reloadables");
        RELOADABLES.forEach((k, v) -> LogHelper.subInfo(k));
        LogHelper.info("Found %d reloadables", RELOADABLES.size());
    }
}
