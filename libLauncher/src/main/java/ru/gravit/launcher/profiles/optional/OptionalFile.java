package ru.gravit.launcher.profiles.optional;

import ru.gravit.launcher.LauncherAPI;

import java.util.Objects;
import java.util.Set;

public class OptionalFile {
    @LauncherAPI
    public String[] list;
    @LauncherAPI
    public OptionalType type;
    @LauncherAPI
    public boolean mark;
    @LauncherAPI
    public boolean visible;
    @LauncherAPI
    public String name;
    @LauncherAPI
    public String info;
    @LauncherAPI
    public OptionalDepend[] dependenciesFile;
    @LauncherAPI
    public OptionalDepend[] conflictFile;
    @LauncherAPI
    public transient OptionalFile[] dependencies;
    @LauncherAPI
    public transient OptionalFile[] conflict;
    @LauncherAPI
    public int subTreeLevel = 1;
    @LauncherAPI
    public long permissions = 0L;
    @LauncherAPI
    public transient Set<OptionalFile> dependenciesCount;

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
}
