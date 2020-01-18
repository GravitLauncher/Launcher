package pro.gravit.launchserver.asm;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import pro.gravit.launcher.LauncherInject;

public class InjectClassAcceptor {
	public static final Class<?>[] primitives = new Class<?>[] { java.lang.Boolean.class, java.lang.Character.class,
			java.lang.Byte.class, java.lang.Short.class, java.lang.Integer.class, java.lang.Long.class,
			java.lang.Float.class, java.lang.Double.class, java.lang.String.class };

	public static final List<Class<?>> zPrimitivesList = Arrays.asList(primitives);
	public static final String INJ_DESC = Type.getDescriptor(LauncherInject.class);

	public static void checkMap(Map<String,Object> object) {
		if (!object.values().stream().allMatch(zPrimitivesList::contains))
			throw new IllegalArgumentException("Only primitives in values...");
	}
	
	public static void visit(ClassNode cn, Map<String, Object> object) {
		cn.fields.stream().filter(e -> e.invisibleAnnotations != null)
				.filter(e -> !e.invisibleAnnotations.isEmpty() && e.invisibleAnnotations.stream().anyMatch(f -> f.desc.equals(INJ_DESC))).forEach(e -> {
					// Notice that fields that will be used with this algo should not have default
					// value by = ...;
					AnnotationNode n = e.invisibleAnnotations.stream().filter(f -> INJ_DESC.equals(f.desc)).findFirst()
							.get();
					AtomicReference<String> valueName = new AtomicReference<>(null);
					n.accept(new AnnotationVisitor(Opcodes.ASM7) {
						@Override
						public void visit(final String name, final Object value) {
							if ("value".equals(name)) {
								if (value.getClass() != String.class)
									throw new IllegalArgumentException(
											"Invalid Annotation with value class " + e.getClass().getName());
								valueName.set(value.toString());
							}
						}
					});
					if (valueName.get() == null)
						throw new IllegalArgumentException("Annotation should always contains 'value' key");
					if (object.containsKey(valueName.get()))
						e.value = object.get(valueName.get());
				});
	}
}
