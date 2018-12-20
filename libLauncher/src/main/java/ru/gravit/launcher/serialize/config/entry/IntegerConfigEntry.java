package ru.gravit.launcher.serialize.config.entry;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;

import java.io.IOException;

public final class IntegerConfigEntry extends ConfigEntry<Integer> {
    @LauncherAPI
    public IntegerConfigEntry(HInput input, boolean ro) throws IOException {
        this(input.readVarInt(), ro, 0);
    }

    @LauncherAPI
    public IntegerConfigEntry(int value, boolean ro, int cc) {
        super(value, ro, cc);
    }

    @Override
    public Type getType() {
        return Type.INTEGER;
    }

    @Override
    public void write(HOutput output) throws IOException {
        output.writeVarInt(getValue());
    }
}
