package ru.gravit.utils;

import ru.gravit.launcher.LauncherAPI;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class Version {
    @LauncherAPI
    public final int major;
    @LauncherAPI
    public final int minor;
    @LauncherAPI
    public final int patch;
    @LauncherAPI
    public final int build;
    @LauncherAPI
    public final Type release;

    @LauncherAPI
    public Version(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        build = 0;
        release = Type.UNKNOWN;
    }

    @LauncherAPI
    public Version(int major, int minor, int patch, int build) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.build = build;
        release = Type.UNKNOWN;
    }

    @LauncherAPI
    public Version(int major, int minor, int patch, int build, Type release) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.build = build;
        this.release = release;
    }

    @Override
    @LauncherAPI
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version that = (Version) o;
        return major == that.major &&
                minor == that.minor &&
                patch == that.patch &&
                build == that.build;
    }

    @LauncherAPI
    public String getVersionString() {
        return String.format("%d.%d.%d", major, minor, patch);
    }

    @Override
    @LauncherAPI
    public int hashCode() {
        return Objects.hash(major, minor, patch, build);
    }

    @LauncherAPI
    public String getReleaseStatus() {
        if (release.equals(Type.UNKNOWN)) return "";
        return release.name().toLowerCase(Locale.ENGLISH);
    }

    @Override
    @LauncherAPI
    public String toString() {
        return String.format("%d.%d.%d-%d %s", major, minor, patch, build, getReleaseStatus());
    }

    @LauncherAPI
    public enum Type {
        LTS,
        STABLE,
        BETA,
        ALPHA,
        DEV,
        EXPERIMENTAL,
        UNKNOWN;

    	private static final Map<String, Type> types = new HashMap<>();
    	public static final Map<String, Type> unModTypes = Collections.unmodifiableMap(types);
        
        static {
        	Arrays.asList(values()).stream().forEach(type -> types.put(type.name().substring(0, type.name().length() < 3 ? type.name().length() : 3).toLowerCase(Locale.ENGLISH), type));
        }
    }
}
