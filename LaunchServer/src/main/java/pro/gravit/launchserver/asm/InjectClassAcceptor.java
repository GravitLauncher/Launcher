package pro.gravit.launchserver.asm;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import pro.gravit.launchserver.binary.BuildContext;
import pro.gravit.launchserver.binary.tasks.MainBuildTask;
import pro.gravit.launcher.LauncherInject;
import pro.gravit.launcher.LauncherInjectionConstructor;

public class InjectClassAcceptor implements MainBuildTask.ASMTransformer {
	private final Map<String, Object> values;

	public InjectClassAcceptor(Map<String, Object> values) {
		this.values = values;
	}
	private static final List<Class<?>> zPrimitivesList = Arrays.asList(java.lang.Boolean.class, java.lang.Character.class,
			java.lang.Byte.class, java.lang.Short.class, java.lang.Integer.class, java.lang.Long.class,
			java.lang.Float.class, java.lang.Double.class, java.lang.String.class);
	private static final String INJ_DESC = Type.getDescriptor(LauncherInject.class);
	private static final String INJ_C_DESC = Type.getDescriptor(LauncherInjectionConstructor.class);
	private static final List<String> cPrimitivesList = Arrays.asList("I", "V", "Z", "B", "C", "S", "D", "F", "J", Type.getDescriptor(String.class));
	
	private static void visit(ClassNode cn, Map<String, Object> object) {
        MethodNode clinit = cn.methods.stream().filter(methodNode ->
        	"<clinit>".equals(methodNode.name)).findFirst().orElseGet(() -> {
        		MethodNode ret = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, "<clinit>", "()V", null, null);
        		ret.instructions.add(new InsnNode(Opcodes.RETURN));
        		cn.methods.add(ret);
        		return ret;
        	});
        List<MethodNode> constructors = cn.methods.stream().filter(e -> "<init>".equals(e.name)).collect(Collectors.toList());
        MethodNode init = constructors.stream().filter(e -> e.invisibleAnnotations.stream().filter(f -> INJ_C_DESC.equals(f.desc)).findFirst()
				.isPresent()).findFirst().orElseGet(() -> constructors.stream().filter(e -> e.desc.equals("()V")).findFirst().orElse(null));
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
					if (object.containsKey(valueName.get())) {
						Object val = object.get(valueName.get());
						if ((e.access & Opcodes.ACC_STATIC) != 0)
							if (cPrimitivesList.contains(e.desc) && zPrimitivesList.contains(val.getClass()))
								e.value = val;
							else {
								List<FieldInsnNode> nodes = Arrays.stream(clinit.instructions.toArray()).filter(p -> p instanceof FieldInsnNode && p.getOpcode() == Opcodes.PUTSTATIC).map(p -> (FieldInsnNode) p)
										.filter(p -> p.owner.equals(cn.name) && p.name.equals(e.name) && p.desc.equals(e.desc)).collect(Collectors.toList());
								InsnList injector = new InsnList();
								pushInjector(injector, val, e);
								if (nodes.isEmpty()) {
									injector.insert(new InsnNode(Opcodes.ICONST_0));
									injector.add(new FieldInsnNode(Opcodes.PUTSTATIC, cn.name, e.name, e.desc));
									Arrays.stream(clinit.instructions.toArray()).filter(p -> p.getOpcode() == Opcodes.RETURN).forEach(p -> clinit.instructions.insertBefore(p, injector));
								} else
									for (FieldInsnNode node : nodes) clinit.instructions.insertBefore(node, injector);
							}
						else {
							if (init == null) throw new IllegalArgumentException("Not found init in target: " + cn.name);
							List<FieldInsnNode> nodes = Arrays.stream(init.instructions.toArray()).filter(p -> p instanceof FieldInsnNode && p.getOpcode() == Opcodes.PUTFIELD).map(p -> (FieldInsnNode) p)
								.filter(p -> p.owner.equals(cn.name) && p.name.equals(e.name) && p.desc.equals(e.desc)).collect(Collectors.toList());
							InsnList injector = new InsnList();
							pushInjector(injector, val, e);
							if (nodes.isEmpty()) {
								injector.insert(new VarInsnNode(Opcodes.ALOAD, 0));
								injector.insert(new InsnNode(Opcodes.ICONST_0));
								injector.add(new FieldInsnNode(Opcodes.PUTSTATIC, cn.name, e.name, e.desc));
								Arrays.stream(init.instructions.toArray()).filter(p -> p.getOpcode() == Opcodes.RETURN).forEach(p -> clinit.instructions.insertBefore(p, injector));
							} else
								for (FieldInsnNode node : nodes) init.instructions.insertBefore(node, injector);
						}
					}
				});
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void pushInjector(InsnList injector, Object val, FieldNode e) {
		injector.add(new InsnNode(Opcodes.POP));
		if (e.desc.equals("Z")) {
			if ((Boolean) val) injector.add(new InsnNode(Opcodes.ICONST_1));
			else injector.add(new InsnNode(Opcodes.ICONST_0));
		} else if (e.desc.equals("C")) {
			injector.add(NodeUtils.push(((Number) val).intValue()));
			injector.add(new InsnNode(Opcodes.I2C));
		}  else if (e.desc.equals("B")) {
			injector.add(NodeUtils.push(((Number) val).intValue()));
			injector.add(new InsnNode(Opcodes.I2B));
		}  else if (e.desc.equals("S")) {
			injector.add(NodeUtils.push(((Number) val).intValue()));
			injector.add(new InsnNode(Opcodes.I2S));
		} else if (e.desc.equals("I")) {
			injector.add(NodeUtils.push(((Number) val).intValue()));
		} else if (e.desc.equals("[B")) {
			serializebArr(injector, (byte[]) val);
		} else if (e.desc.equals("Ljava/util/List;")) {
			if (((List) val).isEmpty()) {
				injector.add(new TypeInsnNode(Opcodes.NEW, "java/util/ArrayList"));
				injector.add(new InsnNode(Opcodes.DUP));
				injector.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V"));
			} else {
				Class<?> c = ((List) val).get(0).getClass();
				if (c == byte[].class)
					serializeListbArr(injector, (List<byte[]>) val);
				else if (c == String.class)
					serializeListString(injector, (List<String>) val);
				else
					throw new UnsupportedOperationException("Unsupported class" + c.getName());
			}
		} else {
			if (!cPrimitivesList.contains(e.desc) || !zPrimitivesList.contains(val.getClass()))
				throw new UnsupportedOperationException("Unsupported class");
			injector.add(new LdcInsnNode(val));
		}
		// TODO Map<String,String>
	}

	private static void serializebArr(InsnList injector, byte[] val) {
		injector.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Base64", "getDecoder", "()Ljava/util/Base64$Decoder;", false));
		injector.add(NodeUtils.getSafeStringInsnList(Base64.getEncoder().encodeToString(val)));
		injector.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Base64$Decoder", "decode", "(Ljava/lang/String;)[B", false));
	}

	private static void serializeListbArr(InsnList inj, List<byte[]> val) {
		inj.add(new TypeInsnNode(Opcodes.NEW, "java/util/ArrayList"));
		inj.add(new InsnNode(Opcodes.DUP)); // +1
		inj.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V"));
        for (byte[] value : val) {
        	inj.add(new InsnNode(Opcodes.DUP)); // +1-1
        	serializebArr(inj, value);
        	inj.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true));
        	inj.add(new InsnNode(Opcodes.POP));
        }
	}

	private static void serializeListString(InsnList inj, List<String> val) {
		inj.add(new TypeInsnNode(Opcodes.NEW, "java/util/ArrayList"));
		inj.add(new InsnNode(Opcodes.DUP)); // +1
		inj.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V"));
        for (String value : val) {
        	inj.add(new InsnNode(Opcodes.DUP)); // +1-1
        	inj.add(NodeUtils.getSafeStringInsnList(value));
        	inj.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true));
        	inj.add(new InsnNode(Opcodes.POP));
        }
	}

	@Override
	public void transform(ClassNode cn, String classname, BuildContext context) {
		visit(cn, values);
	}
}
