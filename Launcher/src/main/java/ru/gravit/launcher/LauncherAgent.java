package ru.gravit.launcher;

import cpw.mods.fml.SafeExitJVMLegacy;
import net.minecraftforge.fml.SafeExitJVM;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import ru.gravit.launcher.utils.NativeJVMHalt;
import ru.gravit.utils.helper.LogHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

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
    	SafeExitJVMLegacy.class.getName();
    	SafeExitJVM.class.getName();
    	NativeJVMHalt.class.getName();
        NativeJVMHalt.initFunc();
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
        try {
        	//replaceClasses(pb, rt);
        } catch (Error e) {
        	NativeJVMHalt.haltA(294);
        	throw e;
        }
    }

    public static boolean isStarted() {
        return isAgentStarted;
    }

    private static void replaceClasses(boolean pb, boolean rt) {
    	java.awt.Robot.class.getName();
    	List<java.lang.instrument.ClassDefinition> defs = new ArrayList<>();
		if(rt) {
			try {
				defs.add(new java.lang.instrument.ClassDefinition(java.lang.Runtime.class, transformClass(java.lang.Runtime.class.getName(), getClassFile(java.lang.Runtime.class))));
			} catch(Exception e) {
				throw new Error(e);
			}
		}
		if(pb) {
			try {
				defs.add(new java.lang.instrument.ClassDefinition(java.lang.ProcessBuilder.class, transformClass(java.lang.ProcessBuilder.class.getName(), getClassFile(java.lang.ProcessBuilder.class))));
			} catch(Exception e) {
				throw new Error(e);
			}
		}
		try {
			defs.add(new java.lang.instrument.ClassDefinition(java.awt.Robot.class, transformClass(java.awt.Robot.class.getName(), getClassFile(java.awt.Robot.class))));
		} catch(Exception e) {
			throw new Error(e);
		}
		try {
			inst.redefineClasses(defs.toArray(new java.lang.instrument.ClassDefinition[0]));
    	} catch(Exception e) {
			throw new Error(e);
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
