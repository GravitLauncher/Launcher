package ru.gravit.launcher;

import ru.gravit.utils.helper.LogHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.*;

@LauncherAPI
public final class LauncherAgent {
    private static boolean isAgentStarted = false;
    public static Instrumentation inst;

    public static void addJVMClassPath(String path) throws IOException {
        LogHelper.debug("Launcher Agent addJVMClassPath");
        inst.appendToSystemClassLoaderSearch(new JarFile(path));
    }

    public boolean isAgentStarted() {
        return isAgentStarted;
    }

    public static void premain(String agentArgument, Instrumentation instrumentation) {
        System.out.println("Launcher Agent");
        inst = instrumentation;
        isAgentStarted = true;
        boolean pb = true;
        boolean rt = true;
        if (agentArgument != null) {
        	String trimmedArg = agentArgument.trim();
        	if (!trimmedArg.isEmpty())  {
        		if (trimmedArg.contains("p")) pb = false;
        		if (trimmedArg.contains("r")) rt = false;
        	}
        }
        if (rt || pb) replaceClasses(pb, rt);
    }

    public static boolean isStarted() {
        return isAgentStarted;
    }
    
    /**
     * @author https://github.com/Konloch/JVM-Sandbox
	 * Replaces the Runtime class via instrumentation, transforms the class via ASM
	 */
    private static void replaceClasses(boolean pb, boolean rt) {
    	java.awt.Robot.class.getName();
		for(Class<?> c : inst.getAllLoadedClasses()) {
			if(rt && c.getName().equals("java.lang.Runtime")) {
				try {
					inst.redefineClasses(new java.lang.instrument.ClassDefinition(java.lang.Runtime.class, transformClass(c.getName(), getClassFile(c))));
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			if(pb && c.getName().equals("java.lang.ProcessBuilder")) {
				try {
					inst.redefineClasses(new java.lang.instrument.ClassDefinition(java.lang.ProcessBuilder.class, transformClass(c.getName(), getClassFile(c))));
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			if(c.getName().equals("java.awt.Robot")) {
				try {
					inst.redefineClasses(new java.lang.instrument.ClassDefinition(java.lang.ProcessBuilder.class, transformClass(c.getName(), getClassFile(c))));
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
     * @author https://github.com/Konloch/JVM-Sandbox
	 * Use ASM to modify the byte array
	 */
	private static byte[] transformClass(String className, byte[] classBytes) {
		if (className.equals("java.lang.Runtime")) {
			ClassReader cr=new ClassReader(classBytes);
			ClassNode cn=new ClassNode();
			cr.accept(cn,ClassReader.EXPAND_FRAMES);
			
			for (Object o : cn.methods.toArray()) {
				MethodNode m = (MethodNode) o;
				if(m.name.equals("exec")) {
					m.instructions.insert(new InsnNode(ARETURN));
					m.instructions.insert(new InsnNode(ACONST_NULL));
				}
			}
			ClassWriter cw=new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
			cn.accept(cw);
			return cw.toByteArray();
		} else if (className.equals("java.lang.ProcessBuilder")) {
			ClassReader cr=new ClassReader(classBytes);
			ClassNode cn=new ClassNode();
			cr.accept(cn,ClassReader.EXPAND_FRAMES);
			
			for (Object o : cn.methods.toArray()) {
				MethodNode m = (MethodNode) o;
				if(m.name.equals("start")) {
					m.instructions.insert(new InsnNode(ARETURN));
					m.instructions.insert(new InsnNode(ACONST_NULL));
				}
			}
			ClassWriter cw=new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
			cn.accept(cw);
			return cw.toByteArray();
		} else if (className.equals("java.awt.Robot")) {
			ClassReader cr=new ClassReader(classBytes);
			ClassNode cn=new ClassNode();
			cr.accept(cn,ClassReader.EXPAND_FRAMES);
			
			for (Object o : cn.methods.toArray()) {
				MethodNode m = (MethodNode) o;
				if(	m.name.equals("createScreenCapture") 	|| m.name.equals("getPixelColor") ||
					m.name.equals("keyPress") 				|| m.name.equals("keyRelease") ||
					m.name.equals("mouseMove")				|| m.name.equals("mousePress") ||
					m.name.equals("mouseWheel"))
				{
					m.instructions.insert(new InsnNode(ARETURN));
					m.instructions.insert(new InsnNode(ACONST_NULL));
				}
			}
			ClassWriter cw=new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
			cn.accept(cw);
			return cw.toByteArray();
		}
		return classBytes;
	}
	
	/**
     * @author https://github.com/Konloch/JVM-Sandbox
     * Do not remove this method. Do not to cause classloading!
	 * Grab the byte array from the loaded Class object
	 * @param clazz
	 * @return array, respending this class in bytecode.
	 * @throws IOException
	 */
	private static byte[] getClassFile(Class<?> clazz) throws IOException {     
	    try (InputStream is = clazz.getResourceAsStream( "/" + clazz.getName().replace('.', '/') + ".class");
	    		ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
	    	int r = 0;
	    	byte[] buffer = new byte[8192];
	    	while((r=is.read(buffer))>=0) {
	        	baos.write(buffer, 0, r);
	    	}   
	    	return baos.toByteArray();
	    }
	}
}
