package pro.gravit.launcher.profiles.optional;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.profiles.optional.actions.OptionalAction;

import java.util.List;
import java.util.Objects;

public class OptionalFile {
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
    public List<pro.gravit.launcher.profiles.optional.triggers.OptionalTrigger> triggersList;
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
    @LauncherNetworkAPI
    public boolean limited;

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

    public String getName() {
        return name;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isMark() {
        return mark;
    }
}
