package ru.gravit.launchserver.binary;

import java.io.IOException;

import javassist.*;

public class JAConfigurator implements AutoCloseable {
    ClassPool pool = ClassPool.getDefault();
    CtClass ctClass;
    CtConstructor ctConstructor;
    CtMethod initModuleMethod;
    String classname;
    StringBuilder body;
    StringBuilder moduleBody;
    int autoincrement;

    public JAConfigurator(Class<?> configclass) throws NotFoundException {
        classname = configclass.getName();
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
        moduleBody.append(" , true );");
        autoincrement++;
    }

    @Override
    public void close() {
        ctClass.defrost();
    }

    public byte[] getBytecode() throws IOException, CannotCompileException {
        body.append("}");
        moduleBody.append("}");
        ctConstructor.setBody(body.toString());
        initModuleMethod.insertAfter(moduleBody.toString());
        return ctClass.toBytecode();
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

    public void setPort(int port) {
        body.append("this.port = ");
        body.append(port);
        body.append(";");
    }

    public ClassPool getPool() {
        return pool;
    }
}
