package pro.gravit.launcher.serialize.stream;

import pro.gravit.launcher.LauncherAPI;
import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.utils.helper.IOHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class StreamObject {
    /* public StreamObject(HInput input) */

    @FunctionalInterface
    public interface Adapter<O extends StreamObject> {
        @LauncherAPI
        O convert(HInput input);
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
