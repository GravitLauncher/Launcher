package pro.gravit.launchserver.binary;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import org.objectweb.asm.Type;

import pro.gravit.launcher.AutogenConfig;
import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launcher.modules.Module;
import pro.gravit.launcher.modules.ModulesManager;
import pro.gravit.launchserver.asm.ClassMetadataReader;
import pro.gravit.launchserver.asm.SafeClassWriter;

public class JAConfigurator {
    private static final String modulesManagerName = Type.getInternalName(ModulesManager.class);
    private static final String registerModDesc = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Module.class));
    private static final String autoGenConfigName = Type.getInternalName(AutogenConfig.class);
    private static final String stringDesc = Type.getDescriptor(String.class);
    private final ClassNode configclass;
    private final MethodNode constructor;
    private final MethodNode initModuleMethod;

    public JAConfigurator(ClassNode configclass) {
        this.configclass = configclass;
        constructor = configclass.methods.stream().filter(e -> "<init>".equals(e.name)).findFirst().get();
        constructor.instructions = new InsnList();
        initModuleMethod = configclass.methods.stream().filter(e -> "initModules".equals(e.name)).findFirst().get();
        initModuleMethod.instructions = new InsnList();
    }

    public void addModuleClass(String fullName) {
        initModuleMethod.instructions.insert(new MethodInsnNode(Opcodes.INVOKEINTERFACE, modulesManagerName, "registerModule", registerModDesc));
        initModuleMethod.instructions.insert(new MethodInsnNode(Opcodes.INVOKESPECIAL, fullName.replace('.', '/'), "<init>", "()V"));
        initModuleMethod.instructions.insert(new TypeInsnNode(Opcodes.NEW, fullName.replace('.', '/')));
    }

    public byte[] getBytecode(ClassMetadataReader reader) {
        ClassWriter cw = new SafeClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        configclass.accept(cw);
        return cw.toByteArray();
    }

    public String getZipEntryPath() {
        return configclass.name.concat(".class");
    }

    public void setAddress(String address) {
        constructor.instructions.add(new LdcInsnNode(address));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, autoGenConfigName, "address", stringDesc));
    }

    public void setProjectName(String name) {
        constructor.instructions.add(new LdcInsnNode(name));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, autoGenConfigName, "projectname", stringDesc));
    }

    public void setSecretKey(String key) {
        constructor.instructions.add(new LdcInsnNode(key));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, autoGenConfigName, "secretKeyClient", stringDesc));
    }

    public void setOemUnlockKey(String key) {
        constructor.instructions.add(new LdcInsnNode(key));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, autoGenConfigName, "oemUnlockKey", stringDesc));
        
    }

    public void setGuardType(String key) {
        constructor.instructions.add(new LdcInsnNode(key));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, autoGenConfigName, "guardType", stringDesc));
    }

    public void push(final int value) {
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
        push(port);
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, autoGenConfigName, "clientPort", Type.INT_TYPE.getInternalName()));
    }

    public void setWarningMissArchJava(boolean b) {
        constructor.instructions.add(new InsnNode(b ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, autoGenConfigName, "isWarningMissArchJava", Type.BOOLEAN_TYPE.getInternalName()));
    }

    public void setGuardLicense(String name, String key, String encryptKey) {
        constructor.instructions.add(new LdcInsnNode(name));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, autoGenConfigName, "guardLicenseName", stringDesc));
        constructor.instructions.add(new LdcInsnNode(key));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, autoGenConfigName, "guardLicenseKey", stringDesc));
        constructor.instructions.add(new LdcInsnNode(encryptKey));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, autoGenConfigName, "guardLicenseEncryptKey", stringDesc));
    }
    
    public void nullGuardLicense() {
        constructor.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, autoGenConfigName, "guardLicenseName", stringDesc));
        constructor.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, autoGenConfigName, "guardLicenseKey", stringDesc));
        constructor.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, autoGenConfigName, "guardLicenseEncryptKey", stringDesc));
    }
}
