package ru.gravit.launcher.serialize.stream;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.utils.helper.IOHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class StreamObject {
    /* public StreamObject(HInput input) */

    @FunctionalInterface
    public interface Adapter<O extends StreamObject> {
        @LauncherAPI
        O convert(HInput input) throws IOException;
    }

    @LauncherAPI
    public final byte[] write() throws IOException {
        try (ByteArrayOutputStream array = IOHelper.newByteArrayOutput()) {
            try (HOutput output = new HOutput(array)) {
                write(output);
            }
            return array.toByteArray();
        }
    }

    @LauncherAPI
    public abstract void write(HOutput output) throws IOException;
}
