package pro.gravit.launcher.hasher;

import java.io.IOException;

import pro.gravit.launcher.LauncherAPI;
import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.stream.EnumSerializer;
import pro.gravit.launcher.serialize.stream.StreamObject;

public abstract class HashedEntry extends StreamObject {
    @LauncherAPI
    public enum Type implements EnumSerializer.Itf {
        DIR(1), FILE(2);
        private static final EnumSerializer<Type> SERIALIZER = new EnumSerializer<>(Type.class);

        public static Type read(HInput input) throws IOException {
            return SERIALIZER.read(input);
        }

        private final int n;

        Type(int n) {
            this.n = n;
        }

        @Override
        public int getNumber() {
            return n;
        }
    }

    @LauncherAPI
    public boolean flag; // For external usage

    @LauncherAPI
    public abstract Type getType();

    @LauncherAPI
    public abstract long size();
}
