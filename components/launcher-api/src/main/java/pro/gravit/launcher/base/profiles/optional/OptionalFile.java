package pro.gravit.launcher.base.profiles.optional;

import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.core.api.features.ProfileFeatureAPI;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class OptionalFile implements ProfileFeatureAPI.OptionalMod {
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
    public List<pro.gravit.launcher.base.profiles.optional.triggers.OptionalTrigger> triggersList;
    @LauncherNetworkAPI
    public OptionalDepend[] dependenciesFile;
    @LauncherNetworkAPI
    public OptionalDepend[] conflictFile;
    @LauncherNetworkAPI
    public OptionalDepend[] groupFile;
    @LauncherNetworkAPI
    public transient OptionalFile[] dependencies;
    @LauncherNetworkAPI
    public transient OptionalFile[] conflict;
    @LauncherNetworkAPI
    public transient OptionalFile[] group;
    @LauncherNetworkAPI
    public int subTreeLevel = 1;
    @LauncherNetworkAPI
    public boolean isPreset;
    @LauncherNetworkAPI
    public boolean limited;

    @LauncherNetworkAPI
    public String category;

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

    @Override
    public String getDescription() {
        return info;
    }

    @Override
    public String getCategory() {
        return category;
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public Set<ProfileFeatureAPI.OptionalMod> getDependencies() {
        return Set.of(dependencies);
    }

    public boolean isMark() {
        return mark;
    }
}
