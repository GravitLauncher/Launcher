package pro.gravit.launcher.hasher;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.stream.EnumSerializer;
import pro.gravit.launcher.serialize.stream.StreamObject;

import java.io.IOException;

public abstract class HashedEntry extends StreamObject {

    @LauncherNetworkAPI
    public boolean flag; // For external usage

    public abstract Type getType();

    public abstract long size();


    public enum Type implements EnumSerializer.Itf {
        DIR(1), FILE(2);
        private static final EnumSerializer<Type> SERIALIZER = new EnumSerializer<>(Type.class);
        private final int n;

        Type(int n) {
            this.n = n;
        }

        public static Type read(HInput input) throws IOException {
            return SERIALIZER.read(input);
        }

        @Override
        public int getNumber() {
            return n;
        }
    }
}
