package ru.gravit.launcher.serialize.config;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launcher.serialize.stream.StreamObject;

import java.io.IOException;
import java.util.Objects;

public abstract class ConfigObject extends StreamObject {
    @FunctionalInterface
    public interface Adapter<O extends ConfigObject> {
        @LauncherAPI
        O convert(BlockConfigEntry entry);
    }

    @LauncherAPI
    public final BlockConfigEntry block;

    @LauncherAPI
    protected ConfigObject(BlockConfigEntry block) {
        this.block = Objects.requireNonNull(block, "block");
    }

    @Override
    public final void write(HOutput output) throws IOException {
        block.write(output);
    }
}
