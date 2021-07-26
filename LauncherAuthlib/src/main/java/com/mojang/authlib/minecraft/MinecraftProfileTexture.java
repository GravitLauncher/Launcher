package com.mojang.authlib.minecraft;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class MinecraftProfileTexture {
    public static final Set<Type> PROFILE_TEXTURE_TYPES = Collections.unmodifiableSet(EnumSet.allOf(Type.class));
    public static final int PROFILE_TEXTURE_COUNT = PROFILE_TEXTURE_TYPES.size();
    // Instance
    private final String url;
    private final String hash;
    private final Map<String, String> metadata;

    public MinecraftProfileTexture(String url) {
        this(url, baseName(url));
    }

    public MinecraftProfileTexture(String url, String hash) {
        this.url = url;
        this.hash = hash;
        this.metadata = null;
    }

    public MinecraftProfileTexture(String url, String hash, Map<String, String> metadata) {
        this.url = url;
        this.hash = hash;
        this.metadata = metadata;
    }

    private static String baseName(String url) {
        String name = url.substring(url.lastIndexOf('/') + 1);

        // Remove index
        int extensionIndex = name.lastIndexOf('.');
        if (extensionIndex >= 0)
            name = name.substring(0, extensionIndex);

        // We're done
        return name;
    }

    public String getHash() {
        return hash;
    }

    public String getMetadata(String key) {
        if (metadata == null) {
            return null;
        }
        return metadata.get(key);
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return String.format("MinecraftProfileTexture{url='%s',hash=%s}", url, hash);
    }

    public enum Type {
        SKIN,
        CAPE,
        ELYTRA
    }
}
