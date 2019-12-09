package pro.gravit.launcher.profiles.optional;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

public class OptionalFile {
    @LauncherNetworkAPI
    public String[] list;
    @LauncherNetworkAPI
    public OptionalType type;
    @LauncherNetworkAPI
    public boolean mark;
    @LauncherNetworkAPI
    public final boolean visible = true;
    @LauncherNetworkAPI
    public String name;
    @LauncherNetworkAPI
    public String info;
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
    public final long permissions = 0L;

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
}
