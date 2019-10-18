package pro.gravit.launchserver.binary;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launchserver.asm.ConfigGenerator;

public class LauncherConfigurator extends ConfigGenerator {
    private static final String modulesManagerName = "pro/gravit/launcher/modules/LauncherModulesManager";
    private static final String launcherName = "pro/gravit/launcher/LauncherEngine";
    private static final String modulesManagerDesc = "Lpro/gravit/launcher/client/ClientModuleManager;";
    private static final String registerModDesc = Type.getMethodDescriptor(Type.getType(LauncherModule.class), Type.getType(LauncherModule.class));
    private final MethodNode initModuleMethod;

    public LauncherConfigurator(ClassNode configclass) {
    	super(configclass);
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

    public void setGuardType(String key) {
        setStringField("guardType", key);
    }

    public void setSecureCheck(String hash, String salt) {
        setStringField("secureCheckHash", hash);
        setStringField("secureCheckSalt", salt);
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
        setIntegerField("env", i);
    }

    public void setClientPort(int port) {
        setIntegerField("clientPort", port);
    }

    public void setWarningMissArchJava(boolean b) {
        setBooleanField("isWarningMissArchJava", b);
    }
}
