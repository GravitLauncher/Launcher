package ru.gravit.launchserver.binary;

import java.io.IOException;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.NotFoundException;

public class JAConfigurator implements AutoCloseable {
    ClassPool pool = ClassPool.getDefault();
    CtClass ctClass;
    CtConstructor ctConstructor;
    String classname;
    StringBuilder body;
    int autoincrement;
    public JAConfigurator(Class<?> configclass) throws NotFoundException {
        classname = configclass.getName();
        ctClass = pool.get(classname);
        ctConstructor = ctClass.getDeclaredConstructor(null);
        body = new StringBuilder("{");
        autoincrement = 0;
    }
    public void addModuleClass(String fullName)
    {
        body.append("Module mod");
        body.append(autoincrement);
        body.append(" = new ");
        body.append(fullName);
        body.append("();");
        body.append("Launcher.modulesManager.registerModule( mod");
        body.append(autoincrement);
        body.append(" , true );");
        autoincrement++;
    }
    @Override
    public void close() {
        ctClass.defrost();
    }
    public byte[] getBytecode() throws IOException, CannotCompileException {
        body.append("}");
        ctConstructor.setBody(body.toString());
        return ctClass.toBytecode();
    }
    public String getZipEntryPath()
    {
        return classname.replace('.','/').concat(".class");
    }
    public void setAddress(String address)
    {
        body.append("this.address = \"");
        body.append(address);
        body.append("\";");
    }

    public void setPort(int port)
    {
        body.append("this.port = ");
        body.append(port);
        body.append(";");
    }
}
