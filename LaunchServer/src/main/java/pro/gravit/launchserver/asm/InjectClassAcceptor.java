package pro.gravit.launchserver.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import pro.gravit.launcher.LauncherInject;
import pro.gravit.launcher.LauncherInjectionConstructor;
import pro.gravit.launchserver.binary.BuildContext;
import pro.gravit.launchserver.binary.tasks.MainBuildTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@SuppressWarnings("rawtypes")
public class InjectClassAcceptor implements MainBuildTask.ASMTransformer {
    private static final List<Class<?>> primitiveLDCClasses = Arrays.asList(java.lang.Integer.class, java.lang.Long.class,
            java.lang.Float.class, java.lang.Double.class, java.lang.String.class);
    private static final String INJECTED_FIELD_DESC = Type.getDescriptor(LauncherInject.class);
    private static final String INJECTED_CONSTRUCTOR_DESC = Type.getDescriptor(LauncherInjectionConstructor.class);
    private static final List<String> primitiveLDCDescriptors = Arrays.asList(Type.INT_TYPE.getDescriptor(), Type.DOUBLE_TYPE.getDescriptor(),
            Type.FLOAT_TYPE.getDescriptor(), Type.LONG_TYPE.getDescriptor(), Type.getDescriptor(String.class));
    private static final Map<Class<?>, Serializer<?>> serializers;

    static {
        serializers = new HashMap<>();
        serializers.put(List.class, new ListSerializer());
        serializers.put(Map.class, new MapSerializer());
        serializers.put(byte[].class, new ByteArraySerializer());
        serializers.put(Short.class, serializerClass(Opcodes.I2S));
        serializers.put(Byte.class, serializerClass(Opcodes.I2B));
        serializers.put(Type.class, (Serializer<Type>) e -> { // ow.Type == java.lang.Class in LDC
            InsnList ret = new InsnList();
            ret.add(new LdcInsnNode(e));
            return ret;
        });
        serializers.put(Boolean.class, (Serializer<Boolean>) e -> {
            InsnList ret = new InsnList();
            ret.add(new InsnNode(e ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            return ret;
        });
        serializers.put(Character.class, (Serializer<Character>) e -> {
            InsnList ret = new InsnList();
            ret.add(NodeUtils.push((int) e));
            ret.add(new InsnNode(Opcodes.I2C));
            return ret;
        });
        serializers.put(Enum.class, (Serializer<Enum>) NodeUtils::makeValueEnumGetter);
    }

    private final Map<String, Object> values;

    public InjectClassAcceptor(Map<String, Object> values) {
        this.values = values;
    }

    private static void visit(ClassNode classNode, Map<String, Object> values) {
        MethodNode clinitMethod = classNode.methods.stream().filter(methodNode -> "<clinit>".equals(methodNode.name))
                .findFirst().orElseGet(() -> {
                    MethodNode newClinitMethod = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                            "<clinit>", "()V", null, null);
                    newClinitMethod.instructions.add(new InsnNode(Opcodes.RETURN));
                    classNode.methods.add(newClinitMethod);
                    return newClinitMethod;
                });
        List<MethodNode> constructors = classNode.methods.stream().filter(method -> "<init>".equals(method.name))
                .collect(Collectors.toList());
        MethodNode initMethod = constructors.stream().filter(method -> method.invisibleAnnotations != null
                && method.invisibleAnnotations.stream().anyMatch(annotation -> INJECTED_CONSTRUCTOR_DESC.equals(annotation.desc))).findFirst()
                .orElseGet(() -> constructors.stream().filter(method -> method.desc.equals("()V")).findFirst().orElse(null));
        classNode.fields.forEach(field -> {
            // Notice that fields that will be used with this algo should not have default
            // value by = ...;
            boolean isStatic = (field.access & Opcodes.ACC_STATIC) != 0;
            injectTo(isStatic ? clinitMethod : initMethod, classNode, field, isStatic, values);
        });
    }

    public static void injectTo(MethodNode initMethod, ClassNode classNode, FieldNode field, boolean isStatic, Map<String, Object> values) {
        AnnotationNode valueAnnotation = field.invisibleAnnotations != null ? field.invisibleAnnotations.stream()
                .filter(annotation -> INJECTED_FIELD_DESC.equals(annotation.desc)).findFirst()
                .orElse(null) : null;
        if (valueAnnotation == null) {
            return;
        }
        field.invisibleAnnotations.remove(valueAnnotation);
        AtomicReference<String> valueName = new AtomicReference<>(null);
        valueAnnotation.accept(new AnnotationVisitor(Opcodes.ASM7) {
            @Override
            public void visit(final String name, final Object value) {
                if ("value".equals(name)) {
                    if (value.getClass() != String.class)
                        throw new IllegalArgumentException(
                                String.format("Invalid annotation with value class %s", field.getClass().getName()));
                    valueName.set(value.toString());
                }
            }
        });
        if (valueName.get() == null) {
            throw new IllegalArgumentException("Annotation should always contains 'value' key");
        }
        if (!values.containsKey(valueName.get())) {
            return;
        }
        Object value = values.get(valueName.get());
        //if ((field.access & Opcodes.ACC_STATIC) != 0) {
        if (isStatic) {
            if (primitiveLDCDescriptors.contains(field.desc) && primitiveLDCClasses.contains(value.getClass())) {
                field.value = value;
                return;
            }
            List<FieldInsnNode> putStaticNodes = Arrays.stream(initMethod.instructions.toArray())
                    .filter(node -> node instanceof FieldInsnNode && node.getOpcode() == Opcodes.PUTSTATIC).map(p -> (FieldInsnNode) p)
                    .filter(node -> node.owner.equals(classNode.name) && node.name.equals(field.name) && node.desc.equals(field.desc)).collect(Collectors.toList());
            InsnList setter = serializeValue(value);
            if (putStaticNodes.isEmpty()) {
                setter.add(new FieldInsnNode(Opcodes.PUTSTATIC, classNode.name, field.name, field.desc));
                Arrays.stream(initMethod.instructions.toArray()).filter(node -> node.getOpcode() == Opcodes.RETURN)
                        .forEach(node -> initMethod.instructions.insertBefore(node, setter));
            } else {
                setter.insert(new InsnNode(Type.getType(field.desc).getSize() == 1 ? Opcodes.POP : Opcodes.POP2));
                for (FieldInsnNode fieldInsnNode : putStaticNodes) {
                    initMethod.instructions.insertBefore(fieldInsnNode, setter);
                }
            }
        } else {
            if (initMethod == null) {
                throw new IllegalArgumentException(String.format("Not found init in target: %s", classNode.name));
            }
            List<FieldInsnNode> putFieldNodes = Arrays.stream(initMethod.instructions.toArray())
                    .filter(node -> node instanceof FieldInsnNode && node.getOpcode() == Opcodes.PUTFIELD).map(p -> (FieldInsnNode) p)
                    .filter(node -> node.owner.equals(classNode.name) && node.name.equals(field.name) && node.desc.equals(field.desc)).collect(Collectors.toList());
            InsnList setter = serializeValue(value);
            if (putFieldNodes.isEmpty()) {
                setter.insert(new VarInsnNode(Opcodes.ALOAD, 0));
                setter.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, field.name, field.desc));
                Arrays.stream(initMethod.instructions.toArray())
                        .filter(node -> node.getOpcode() == Opcodes.RETURN)
                        .forEach(node -> initMethod.instructions.insertBefore(node, setter));
            } else {
                setter.insert(new InsnNode(Type.getType(field.desc).getSize() == 1 ? Opcodes.POP : Opcodes.POP2));
                for (FieldInsnNode fieldInsnNode : putFieldNodes) {
                    initMethod.instructions.insertBefore(fieldInsnNode, setter);
                }
            }
        }
    }

    private static Serializer<?> serializerClass(int opcode) {
        return (Serializer<Number>) value -> {
            InsnList ret = new InsnList();
            ret.add(NodeUtils.push(value.intValue()));
            ret.add(new InsnNode(opcode));
            return ret;
        };
    }

    @SuppressWarnings("unchecked")
    private static InsnList serializeValue(Object value) {
        if (value == null) {
            InsnList insnList = new InsnList();
            insnList.add(new InsnNode(Opcodes.ACONST_NULL));
            return insnList;
        }
        if (primitiveLDCClasses.contains(value.getClass())) {
            InsnList insnList = new InsnList();
            insnList.add(new LdcInsnNode(value));
            return insnList;
        }
        for (Map.Entry<Class<?>, Serializer<?>> serializerEntry : serializers.entrySet()) {
            if (serializerEntry.getKey().isInstance(value)) {
                return ((Serializer) serializerEntry.getValue()).serialize(value);
            }
        }
        throw new UnsupportedOperationException(String.format("Serialization of type %s is not supported",
                value.getClass()));
    }

    public static boolean isSerializableValue(Object value) {
        if (value == null) return true;
        if (primitiveLDCClasses.contains(value.getClass())) return true;
        for (Map.Entry<Class<?>, Serializer<?>> serializerEntry : serializers.entrySet()) {
            if (serializerEntry.getKey().isInstance(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void transform(ClassNode classNode, String className, BuildContext context) {
        visit(classNode, values);
    }

    @FunctionalInterface
    private interface Serializer<T> {
        InsnList serialize(T value);
    }

    private static class ListSerializer implements Serializer<List> {
        @Override
        public InsnList serialize(List value) {
            InsnList insnList = new InsnList();
            insnList.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(ArrayList.class)));
            insnList.add(new InsnNode(Opcodes.DUP));
            insnList.add(NodeUtils.push(value.size()));
            insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, Type.getInternalName(ArrayList.class), "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE), false));
            for (Object object : value) {
                insnList.add(new InsnNode(Opcodes.DUP));
                insnList.add(serializeValue(object));
                insnList.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "add",
                        Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)), true));
                insnList.add(new InsnNode(Opcodes.POP));
            }
            return insnList;
        }
    }

    private static class MapSerializer implements Serializer<Map> {
        @Override
        public InsnList serialize(Map value) {
            InsnList insnList = new InsnList();
            insnList.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(value.getClass())));
            insnList.add(new InsnNode(Opcodes.DUP));
            insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, Type.getInternalName(value.getClass()), "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE), false));
            for (Object entryObject : value.entrySet()) {
                Map.Entry entry = (Map.Entry) entryObject;
                insnList.add(new InsnNode(Opcodes.DUP));
                insnList.add(serializeValue(entry.getKey()));
                insnList.add(serializeValue(entry.getValue()));
                insnList.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class), "put",
                        Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class), Type.getType(Object.class)),
                        true));
                insnList.add(new InsnNode(Opcodes.POP));
            }
            return insnList;
        }
    }

    private static class ByteArraySerializer implements Serializer<byte[]> {
        @Override
        public InsnList serialize(byte[] value) {
            InsnList insnList = new InsnList();
            insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(Base64.class),
                    "getDecoder", Type.getMethodDescriptor(Type.getType(Base64.Decoder.class)), false));
            insnList.add(NodeUtils.getSafeStringInsnList(Base64.getEncoder().encodeToString(value)));
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Base64.Decoder.class),
                    "decode", Type.getMethodDescriptor(Type.getType(byte[].class), Type.getType(String.class)),
                    false));
            return insnList;
        }
    }
}