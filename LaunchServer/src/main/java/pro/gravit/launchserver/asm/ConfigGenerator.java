package pro.gravit.launchserver.asm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Base64;
import java.util.List;

public class ConfigGenerator {
    protected static final String stringDesc = Type.getDescriptor(String.class);
    protected static final String byteArrDesc = Type.getDescriptor(byte[].class);
    protected static final String base64DecDesc = "(" + stringDesc + ")" + byteArrDesc;
    protected final ClassNode configclass;
    protected final MethodNode constructor;

    public ConfigGenerator(ClassNode configclass) {
        this.configclass = configclass;
        constructor = this.configclass.methods.stream().filter(e -> "<init>".equals(e.name)).findFirst().get();
        constructor.instructions = new InsnList();
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V"));
    }

    public byte[] getBytecode(ClassMetadataReader reader) {
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));
        ClassWriter cw = new SafeClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        configclass.accept(cw);
        return cw.toByteArray();
    }

    public String getZipEntryPath() {
        return configclass.name.concat(".class");
    }

    public void setStringField(String name, String value) {
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(NodeUtils.getSafeStringInsnList(value));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, configclass.name, name, stringDesc));
    }

    public void setByteArrayField(String name, byte[] value) {
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Base64", "getDecoder", "()Ljava/util/Base64$Decoder;", false));
        constructor.instructions.add(NodeUtils.getSafeStringInsnList(Base64.getEncoder().encodeToString(value)));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Base64$Decoder", "decode", base64DecDesc, false));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, configclass.name, name, stringDesc));
    }

    public void setByteArrayListField(String name, List<byte[]> b) {
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/util/ArrayList"));
        constructor.instructions.add(new InsnNode(Opcodes.DUP));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V"));
        for (byte[] value : b) {
            constructor.instructions.add(new InsnNode(Opcodes.DUP)); // +1-1
            constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Base64", "getDecoder", "()Ljava/util/Base64$Decoder;", false));
            constructor.instructions.add(NodeUtils.getSafeStringInsnList(Base64.getEncoder().encodeToString(value)));
            constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Base64$Decoder", "decode", base64DecDesc, false));
            constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true));
            constructor.instructions.add(new InsnNode(Opcodes.POP));
        }
        constructor.instructions.add(new InsnNode(Opcodes.POP)); // needs -1
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, configclass.name, name, "Ljava/util/List;"));
    }

    public void setIntegerField(String name, int value) {
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(NodeUtils.push(value));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, configclass.name, name, Type.INT_TYPE.getInternalName()));
    }

    public void setLongField(String name, long value) {
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new LdcInsnNode(value));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, configclass.name, name, Type.INT_TYPE.getInternalName()));
    }

    public void setBooleanField(String name, boolean b) {
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new InsnNode(b ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, configclass.name, name, Type.BOOLEAN_TYPE.getInternalName()));
    }
}
