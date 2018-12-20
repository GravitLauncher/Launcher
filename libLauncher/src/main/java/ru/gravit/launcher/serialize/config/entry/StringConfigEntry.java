package ru.gravit.launcher.serialize.config.entry;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;

import java.io.IOException;

public final class StringConfigEntry extends ConfigEntry<String> {
    @LauncherAPI
    public StringConfigEntry(HInput input, boolean ro) throws IOException {
        this(input.readString(0), ro, 0);
    }

    @LauncherAPI
    public StringConfigEntry(String value, boolean ro, int cc) {
        super(value, ro, cc);
    }

    @Override
    public Type getType() {
        return Type.STRING;
    }

    @Override
    protected void uncheckedSetValue(String value) {
        super.uncheckedSetValue(value);
    }

    @Override
    public void write(HOutput output) throws IOException {
        output.writeString(getValue(), 0);
    }
}
