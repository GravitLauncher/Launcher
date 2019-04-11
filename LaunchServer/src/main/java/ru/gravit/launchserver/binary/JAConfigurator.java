package ru.gravit.launchserver.binary;

import javassist.*;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launchserver.binary.tasks.MainBuildTask;

import java.io.IOException;

public class JAConfigurator implements AutoCloseable {
    public ClassPool pool;
    public CtClass ctClass;
    public CtConstructor ctConstructor;
    public CtMethod initModuleMethod;
    String classname;
    StringBuilder body;
    StringBuilder moduleBody;
    int autoincrement;

    public JAConfigurator(String configclass, MainBuildTask jarLauncherBinary) throws NotFoundException {
        pool = new ClassPool(false);
        pool.appendSystemPath();
        classname = configclass;
        ctClass = pool.get(classname);
        ctConstructor = ctClass.getDeclaredConstructor(null);
        initModuleMethod = ctClass.getDeclaredMethod("initModules");
        body = new StringBuilder("{ isInitModules = false; ");
        moduleBody = new StringBuilder("{ isInitModules = true; ");
        autoincrement = 0;
    }

    public void addModuleClass(String fullName) {
        moduleBody.append("ru.gravit.launcher.modules.Module mod");
        moduleBody.append(autoincrement);
        moduleBody.append(" = new ");
        moduleBody.append(fullName);
        moduleBody.append("();");
        moduleBody.append("ru.gravit.launcher.Launcher.modulesManager.registerModule( mod");
        moduleBody.append(autoincrement);
        moduleBody.append(");");
        autoincrement++;
    }

    @Override
    public void close() {
        ctClass.defrost();
    }

    public CtClass getCtClass() {
        return ctClass;
    }

    public byte[] getBytecode() throws IOException, CannotCompileException {
        return ctClass.toBytecode();
    }

    public void compile() throws CannotCompileException {
        body.append("}");
        moduleBody.append("}");
        ctConstructor.setBody(body.toString());
        initModuleMethod.insertAfter(moduleBody.toString());
        if (ctClass.isFrozen()) ctClass.defrost();
    }

    public String getZipEntryPath() {
        return classname.replace('.', '/').concat(".class");
    }

    public void setAddress(String address) {
        body.append("this.address = \"");
        body.append(address);
        body.append("\";");
    }

    public void setProjectName(String name) {
        body.append("this.projectname = \"");
        body.append(name);
        body.append("\";");
    }

    public void setSecretKey(String key) {
        body.append("this.secretKeyClient = \"");
        body.append(key);
        body.append("\";");
    }

    public void setGuardType(String key) {
        body.append("this.guardType = \"");
        body.append(key);
        body.append("\";");
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
        body.append("this.env = ");
        body.append(i);
        body.append(";");
    }

    public void setClientPort(int port) {
        body.append("this.clientPort = ");
        body.append(port);
        body.append(";");
    }

    public void setWarningMissArchJava(boolean b) {
        body.append("this.isWarningMissArchJava = ");
        body.append(b ? "true" : "false");
        body.append(";");
    }

    public void setGuardLicense(String name, String key, String encryptKey) {
        body.append("this.guardLicenseName = \"");
        body.append(name);
        body.append("\";");
        body.append("this.guardLicenseKey = \"");
        body.append(key);
        body.append("\";");
        body.append("this.guardLicenseEncryptKey = \"");
        body.append(encryptKey);
        body.append("\";");
    }

    public ClassPool getPool() {
        return pool;
    }
}
