package ru.gravit.launcher.serialize.config.entry;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;

public final class BlockConfigEntry extends ConfigEntry<Map<String, ConfigEntry<?>>> {
    private static Map<String, ConfigEntry<?>> readMap(HInput input, boolean ro) throws IOException {
        int entriesCount = input.readLength(0);
        Map<String, ConfigEntry<?>> map = new LinkedHashMap<>(entriesCount);
        for (int i = 0; i < entriesCount; i++) {
            String name = VerifyHelper.verifyIDName(input.readString(255));
            ConfigEntry<?> entry = readEntry(input, ro);

            // Try add entry to map
            VerifyHelper.putIfAbsent(map, name, entry, String.format("Duplicate config entry: '%s'", name));
        }
        return map;
    }

    @LauncherAPI
    public BlockConfigEntry(HInput input, boolean ro) throws IOException {
        super(readMap(input, ro), ro, 0);
    }

    @LauncherAPI
    public BlockConfigEntry(int cc) {
        super(Collections.emptyMap(), false, cc);
    }

    @LauncherAPI
    public BlockConfigEntry(Map<String, ConfigEntry<?>> map, boolean ro, int cc) {
        super(map, ro, cc);
    }

    @LauncherAPI
    public void clear() {
        super.getValue().clear();
    }

    @LauncherAPI
    public <E extends ConfigEntry<?>> E getEntry(String name, Class<E> clazz) {
        Map<String, ConfigEntry<?>> map = super.getValue();
        ConfigEntry<?> value = map.get(name);
        if (!clazz.isInstance(value))
            throw new NoSuchElementException(name);
        return clazz.cast(value);
    }

    @LauncherAPI
    public <V, E extends ConfigEntry<V>> V getEntryValue(String name, Class<E> clazz) {
        return getEntry(name, clazz).getValue();
    }

    @Override
    public Type getType() {
        return Type.BLOCK;
    }

    @Override
    public Map<String, ConfigEntry<?>> getValue() {
        Map<String, ConfigEntry<?>> value = super.getValue();
        return ro ? value : Collections.unmodifiableMap(value); // Already RO
    }

    @LauncherAPI
    public boolean hasEntry(String name) {
        return getValue().containsKey(name);
    }

    @LauncherAPI
    public void remove(String name) {
        super.getValue().remove(name);
    }

    @LauncherAPI
    public void setEntry(String name, ConfigEntry<?> entry) {
        super.getValue().put(VerifyHelper.verifyIDName(name), entry);
    }

    @Override
    protected void uncheckedSetValue(Map<String, ConfigEntry<?>> value) {
        Map<String, ConfigEntry<?>> newValue = new LinkedHashMap<>(value);
        newValue.keySet().stream().forEach(VerifyHelper::verifyIDName);

        // Call super method to actually set new value
        super.uncheckedSetValue(ro ? Collections.unmodifiableMap(newValue) : newValue);
    }

    @Override
    public void write(HOutput output) throws IOException {
        Set<Entry<String, ConfigEntry<?>>> entries = getValue().entrySet();
        output.writeLength(entries.size(), 0);
        for (Entry<String, ConfigEntry<?>> mapEntry : entries) {
            output.writeString(mapEntry.getKey(), 255);
            writeEntry(mapEntry.getValue(), output);
        }
    }
}
