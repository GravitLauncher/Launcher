package ru.gravit.launcher;

import java.io.IOException;
import java.util.Objects;

import ru.gravit.utils.helper.IOHelper;

public class LauncherVersion {
    public static int MAJOR = 4;
    public static int MINOR = 0;
    public static int PATCH = 0;
    public static int BUILD = readBuildNumber();
    public static Type RELEASE = Type.DEV;

    public static LauncherVersion getVersion() {
        return new LauncherVersion(MAJOR,MINOR,PATCH,BUILD,RELEASE);
    }
    static int readBuildNumber() {
        try {
            return Integer.valueOf(IOHelper.request(IOHelper.getResourceURL("buildnumber")));
        } catch (IOException ignored) {
            return 0; // Maybe dev env?
        }
    }
    public final int major;
    public final int minor;

    public final int patch;

    public final int build;
    public final Type release;

    public LauncherVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        build = 0;
        release = Type.UNKNOWN;
    }

    public LauncherVersion(int major, int minor, int patch,int build) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.build = build;
        release = Type.UNKNOWN;
    }
    public LauncherVersion(int major, int minor, int patch,int build,Type release) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.build = build;
        this.release = release;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LauncherVersion that = (LauncherVersion) o;
        return major == that.major &&
                minor == that.minor &&
                patch == that.patch &&
                build == that.build;
    }
    
    public String getVersionString() {
    	return String.format("%d.%d.%d", major, minor, patch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, build);
    }
    public String getReleaseStatus()
    {
        String result;
        switch (release) {
            case LTS:
                result="lts";
                break;
            case STABLE:
                result="stable";
                break;
            case BETA:
                result="beta";
                break;
            case ALPHA:
                result="alpha";
                break;
            case DEV:
                result="dev";
                break;
            case EXPERIMENTAL:
                result="experimental";
                break;
            case UNKNOWN:
                result="";
                break;
            default:
                result="";
                break;
        }
        return result;
    }
    @Override
    public String toString() {
        return String.format("%d.%d.%d-%d %s", major, minor, patch, build,getReleaseStatus());
    }
    public enum Type
    {
        LTS,
        STABLE,
        BETA,
        ALPHA,
        DEV,
        EXPERIMENTAL,
        UNKNOWN
    }
}
