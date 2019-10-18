package pro.gravit.launchserver.asm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ConfigGenerator {
	protected static final String stringDesc = Type.getDescriptor(String.class);
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
    
    public void setStringField(String name, String value)
    {
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(NodeUtils.getSafeStringInsnList(value));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, configclass.name, name, stringDesc));
    }

    public void setIntegerField(String name, int value)
    {
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(NodeUtils.push(value));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, configclass.name, name, Type.INT_TYPE.getInternalName()));
    }

    public void setLongField(String name, long value)
    {
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new LdcInsnNode(value));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, configclass.name, name, Type.INT_TYPE.getInternalName()));
    }

    public void setBooleanField(String name, boolean b)
    {
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new InsnNode(b ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, configclass.name, name, Type.BOOLEAN_TYPE.getInternalName()));
    }
}
