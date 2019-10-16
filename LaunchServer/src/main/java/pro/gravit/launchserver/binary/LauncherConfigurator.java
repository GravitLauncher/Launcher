package pro.gravit.launchserver.binary;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import pro.gravit.launcher.AutogenConfig;
import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launchserver.asm.ClassMetadataReader;
import pro.gravit.launchserver.asm.SafeClassWriter;

public class LauncherConfigurator {
    private static final String modulesManagerName = "pro/gravit/launcher/modules/LauncherModulesManager";
    private static final String launcherName = "pro/gravit/launcher/LauncherEngine";
    private static final String modulesManagerDesc = "Lpro/gravit/launcher/client/ClientModuleManager;";
    private static final String registerModDesc = Type.getMethodDescriptor(Type.getType(LauncherModule.class), Type.getType(LauncherModule.class));
    private static final String autoGenConfigName = Type.getInternalName(AutogenConfig.class);
    private static final String stringDesc = Type.getDescriptor(String.class);
    private final ClassNode configclass;
    private final MethodNode constructor;
    private final MethodNode initModuleMethod;

    public LauncherConfigurator(ClassNode configclass) {
        this.configclass = configclass;
        constructor = configclass.methods.stream().filter(e -> "<init>".equals(e.name)).findFirst().get();
        constructor.instructions = new InsnList();
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V"));
        initModuleMethod = configclass.methods.stream().filter(e -> "initModules".equals(e.name)).findFirst().get();
        initModuleMethod.instructions = new InsnList();
    }

    public void addModuleClass(String fullName) {
        initModuleMethod.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, launcherName, "modulesManager", modulesManagerDesc));
        initModuleMethod.instructions.add(new TypeInsnNode(Opcodes.NEW, fullName.replace('.', '/')));
        initModuleMethod.instructions.add(new InsnNode(Opcodes.DUP));
        initModuleMethod.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, fullName.replace('.', '/'), "<init>", "()V"));
        initModuleMethod.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, modulesManagerName, "loadModule", registerModDesc));
    }

    public byte[] getBytecode(ClassMetadataReader reader) {
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));
        initModuleMethod.instructions.add(new InsnNode(Opcodes.RETURN));
        ClassWriter cw = new SafeClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        configclass.accept(cw);
        return cw.toByteArray();
    }

    public String getZipEntryPath() {
        return configclass.name.concat(".class");
    }

    public void setAddress(String address) {
        setStringField("address", address);
    }

    public void setPasswordEncryptKey(String pass) {
        setStringField("passwordEncryptKey", pass);
    }

    public void setProjectName(String name) {
        setStringField("projectname", name);
    }

    public void setSecretKey(String key) {
        setStringField("secretKeyClient", key);
    }

    public void setOemUnlockKey(String key) {
        setStringField("oemUnlockKey", key);
    }

    private void setStringField(String name, String value)
    {
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new LdcInsnNode(value));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, autoGenConfigName, name, stringDesc));
    }

    public void setGuardType(String key) {
        setStringField("guardType", key);
    }
    public void setSecureCheck(String hash, String salt) {
        setStringField("secureCheckHash", hash);
        setStringField("secureCheckSalt", salt);
    }

    private void push(final int value) {
        if (value >= -1 && value <= 5)
            constructor.instructions.add(new InsnNode(Opcodes.ICONST_0 + value));
        else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE)
            constructor.instructions.add(new IntInsnNode(Opcodes.BIPUSH, value));
        else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE)
            constructor.instructions.add(new IntInsnNode(Opcodes.SIPUSH, value));
        else
            constructor.instructions.add(new LdcInsnNode(value));
    }
    
    public void setEnv(LauncherConfig.LauncherEnvironment env) {
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        int i = 2;
        
        switch (env) {
            case DEV:
                i = 0;
                break;
            case DEBUG:
                i = 1;
                break;
            case STD:
                i = 2;
                break;
            case PROD:
                i = 3;
                break;
        }
        push(i);
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, autoGenConfigName, "env", Type.INT_TYPE.getInternalName()));
    }

    public void setClientPort(int port) {
        setIntegerField("clientPort", port);
    }

    public void setIntegerField(String name, int value)
    {
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        push(value);
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, autoGenConfigName, name, Type.INT_TYPE.getInternalName()));
    }

    public void setWarningMissArchJava(boolean b) {
        setBooleanField("isWarningMissArchJava", b);
    }

    private void setBooleanField(String name, boolean b)
    {
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new InsnNode(b ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, autoGenConfigName, name, Type.BOOLEAN_TYPE.getInternalName()));
    }
}
