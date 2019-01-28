package ru.gravit.launcher.profiles.optional;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
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
    public boolean visible = true;
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
    @LauncherAPI
    public OptionalType getType() {
        return OptionalType.FILE;
    }
    @LauncherAPI
    public String getName() {
        return name;
    }
    @LauncherAPI
    public boolean isVisible() {
        return visible;
    }
    @LauncherAPI
    public boolean isMark() {
        return mark;
    }
    @LauncherAPI
    public long getPermissions() {
        return permissions;
    }
    @LauncherAPI
    public void writeType(HOutput output) throws IOException
    {
        switch(type)
        {

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
    @LauncherAPI
    public static OptionalType readType(HInput input) throws IOException
    {
        int t = input.readInt();
        OptionalType type;
        switch(t)
        {
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
                LogHelper.error("readType failed. Read int %d",t);
                type = OptionalType.FILE;
                break;
        }
        return type;
    }
}
