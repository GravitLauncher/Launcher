package ru.gravit.launcher.serialize.config.entry;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;

import java.io.IOException;

public final class BooleanConfigEntry extends ConfigEntry<Boolean> {
    @LauncherAPI
    public BooleanConfigEntry(boolean value, boolean ro, int cc) {
        super(value, ro, cc);
    }

    @LauncherAPI
    public BooleanConfigEntry(HInput input, boolean ro) throws IOException {
        this(input.readBoolean(), ro, 0);
    }

    @Override
    public Type getType() {
        return Type.BOOLEAN;
    }

    @Override
    public void write(HOutput output) throws IOException {
        output.writeBoolean(getValue());
    }
}
