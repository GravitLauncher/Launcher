package pro.gravit.launchserver.asm;

import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import pro.gravit.utils.helper.IOHelper;

public final class NodeUtils {
	private NodeUtils() { }
	public static ClassNode forClass(Class<?> cls, int flags) {
		try (InputStream in = cls.getClassLoader().getResourceAsStream(cls.getName().replace('.', '/') + ".class")) {
			ClassNode ret = new ClassNode();
			new ClassReader(IOHelper.read(in)).accept(ret, flags);
			return ret;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
