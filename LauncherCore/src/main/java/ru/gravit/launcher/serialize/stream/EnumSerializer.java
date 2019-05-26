package ru.gravit.launcher.serialize.stream;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.stream.EnumSerializer.Itf;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class EnumSerializer<E extends Enum<?> & Itf> {
    @FunctionalInterface
    public interface Itf {
        @LauncherAPI
        int getNumber();
    }

    @LauncherAPI
    public static void write(HOutput output, Itf itf) throws IOException {
        output.writeVarInt(itf.getNumber());
    }

    private final Map<Integer, E> map = new HashMap<>(16);

    @LauncherAPI
    public EnumSerializer(Class<E> clazz) {
        for (E e : clazz.getEnumConstants())
            VerifyHelper.putIfAbsent(map, e.getNumber(), e, "Duplicate number for enum constant " + e.name());
    }

    @LauncherAPI
    public E read(HInput input) throws IOException {
        int n = input.readVarInt();
        return VerifyHelper.getMapValue(map, n, "Unknown enum number: " + n);
    }
}
