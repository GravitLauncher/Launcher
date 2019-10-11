package pro.gravit.launcher.patches;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import pro.gravit.launcher.LauncherAgent;

public class FMLPatcher implements ClassFileTransformer {
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		try {
			if (className.startsWith("java") || className.startsWith("sun") || className.startsWith("com/sun") || className.startsWith("javafx")) return classfileBuffer;
			ClassReader cr = new ClassReader(classfileBuffer);
			if ("java/lang/SecurityManager".equals(cr.getSuperName()) && (className.contains("cpw") || className.contains("mods") || className.contains("forge"))) {
				ClassNode cn = new ClassNode();
				cr.accept(cn, ClassReader.EXPAND_FRAMES);
				for (MethodNode m : cn.methods)
					if (m.name.equals("checkPermission") && m.desc.equals("(Ljava/lang/String;)V")) {
						m.instructions.clear();
						m.instructions.insert(new InsnNode(Opcodes.RETURN));
					}
				ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
				cn.accept(cw);
				return cw.toByteArray();
			}
		} catch (Throwable e) { }
		return classfileBuffer;
	}
	public static void apply() {
		LauncherAgent.inst.addTransformer(new FMLPatcher());
	}
}
