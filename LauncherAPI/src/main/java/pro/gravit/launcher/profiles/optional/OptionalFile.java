package pro.gravit.launcher.profiles.optional;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class OptionalFile {
    @LauncherNetworkAPI
    public long permissions = 0L;
    @LauncherNetworkAPI
    @Deprecated
    public String[] list;
    @LauncherNetworkAPI
    @Deprecated
    public OptionalType type;
    @LauncherNetworkAPI
    public List<OptionalAction> actions;
    @LauncherNetworkAPI
    public boolean mark;
    @LauncherNetworkAPI
    public boolean visible = true;
    @LauncherNetworkAPI
    public String name;
    @LauncherNetworkAPI
    public String info;
    @LauncherNetworkAPI
    public OptionalTrigger[] triggers;
    @LauncherNetworkAPI
    public OptionalDepend[] dependenciesFile;
    @LauncherNetworkAPI
    public OptionalDepend[] conflictFile;
    @LauncherNetworkAPI
    public transient OptionalFile[] dependencies;
    @LauncherNetworkAPI
    public transient OptionalFile[] conflict;
    @LauncherNetworkAPI
    public int subTreeLevel = 1;
    @LauncherNetworkAPI
    public boolean isPreset;
    @Deprecated
    public transient Set<OptionalFile> dependenciesCount;
    private volatile transient Collection<BiConsumer<OptionalFile, Boolean>> watchList = null;

    public static OptionalType readType(HInput input) throws IOException {
        int t = input.readInt();
        OptionalType type;
        switch (t) {
            case 1:
                type = OptionalType.FILE;
                break;
            case 2:
                type = OptionalType.CLASSPATH;
                break;
            case 3:
                type = OptionalType.JVMARGS;
                break;
            case 4:
                type = OptionalType.CLIENTARGS;
                break;
            default:
                LogHelper.error("readType failed. Read int %d", t);
                type = OptionalType.FILE;
                break;
        }
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OptionalFile that = (OptionalFile) o;
        return Objects.equals(name, that.name);
    }

    public int hashCode() {
        return Objects.hash(name);
    }

    public OptionalType getType() {
        return OptionalType.FILE;
    }

    public String getName() {
        return name;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isMark() {
        return mark;
    }

    public long getPermissions() {
        return permissions;
    }

    public void writeType(HOutput output) throws IOException {
        switch (type) {

            case FILE:
                output.writeInt(1);
                break;
            case CLASSPATH:
                output.writeInt(2);
                break;
            case JVMARGS:
                output.writeInt(3);
                break;
            case CLIENTARGS:
                output.writeInt(4);
                break;
            default:
                output.writeInt(5);
                break;
        }
    }

    public void registerWatcher(BiConsumer<OptionalFile, Boolean> watcher) {
        if (watchList == null) watchList = ConcurrentHashMap.newKeySet();
        watchList.add(watcher);
    }

    public void removeWatcher(BiConsumer<OptionalFile, Boolean> watcher) {
        if (watchList == null) return;
        watchList.remove(watcher);
    }

    public void clearAllWatchers() {
        if (watchList == null) return;
        watchList.clear();
    }

    public void watchEvent(boolean isMark) {
        if (watchList == null) return;
        watchList.forEach((e) -> e.accept(this, isMark));
    }
}
