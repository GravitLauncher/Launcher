package pro.gravit.launchserver.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JarHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public final class NodeUtils {

    public static final int MAX_SAFE_BYTE_COUNT = 65535 - Byte.MAX_VALUE;

    private NodeUtils() {
    }

    public static ClassNode forClass(Class<?> cls, int flags) {
        try (InputStream in = JarHelper.getClassBytesStream(cls)) {
            ClassNode ret = new ClassNode();
            new ClassReader(IOHelper.read(in)).accept(ret, flags);
            return ret;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ClassNode forClass(String clazz, int flags, ClassMetadataReader r) {
        try {
            ClassNode ret = new ClassNode();
            r.acceptVisitor(clazz, ret, flags);
            return ret;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<AnnotationNode> annots(String clazz, String method, ClassMetadataReader r) {
        if (clazz.startsWith("L")) clazz = Type.getType(clazz).getInternalName();
        try {
            List<AnnotationNode> ret = new ArrayList<>();
            ClassNode n = forClass(clazz, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG, r);
            if (n.visibleAnnotations != null) ret.addAll(n.visibleAnnotations);
            if (n.invisibleAnnotations != null) ret.addAll(n.invisibleAnnotations);
            for (MethodNode m : n.methods)
                if (method.equals(m.name)) {
                    if (m.visibleAnnotations != null) ret.addAll(m.visibleAnnotations);
                    if (m.invisibleAnnotations != null) ret.addAll(m.invisibleAnnotations);
                }
            return ret;
        } catch (Throwable e) {
            return Collections.emptyList();
        }
    }

    private static int doMethodEmulation(String desc) {
        int result = 0;
        Type returnType = Type.getReturnType(desc);

        if (returnType.getSort() == Type.LONG || returnType.getSort() == Type.DOUBLE)
            result++;
        if (returnType.getSort() != Type.VOID)
            result++;

        return result;
    }

    public static int opcodeEmulation(AbstractInsnNode e) {
        int stackSize = 0;
        switch (e.getOpcode()) {
            case NOP:
            case LALOAD: // (index, arrayref) -> (long, long_top)
            case DALOAD: // (index, arrayref) -> (double, double_top)
            case SWAP: // (value1, value2) -> (value2, value1)
            case INEG:
            case LNEG:
            case FNEG:
            case DNEG:
            case IINC:
            case I2F:
            case L2D:
            case F2I:
            case D2L:
            case I2B:
            case I2C:
            case I2S:
            case GOTO:
            case RETURN:
            case NEWARRAY:
            case ANEWARRAY:
            case ARRAYLENGTH:
            case CHECKCAST:
            case INSTANCEOF:
                // Does nothing
                break;
            case ACONST_NULL:
            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
            case BIPUSH:
            case SIPUSH:
            case ILOAD:
            case FLOAD:
            case ALOAD:
            case DUP:
            case DUP_X1:
            case DUP_X2:
            case I2L:
            case I2D:
            case F2L:
            case F2D:
            case NEW:
                // Pushes one-word constant to stack
                stackSize++;
                break;
            case LDC:
                LdcInsnNode ldc = (LdcInsnNode) e;
                if (ldc.cst instanceof Long || ldc.cst instanceof Double)
                    stackSize++;

                stackSize++;
                break;
            case LCONST_0:
            case LCONST_1:
            case DCONST_0:
            case DCONST_1:
            case LLOAD:
            case DLOAD:
            case DUP2:
            case DUP2_X1:
            case DUP2_X2:
                // Pushes two-word constant or two one-word constants to stack
                stackSize++;
                stackSize++;
                break;
            case INVOKEVIRTUAL:
            case INVOKESPECIAL:
            case INVOKEINTERFACE:
                stackSize += doMethodEmulation(((MethodInsnNode) e).desc);
                break;
            case INVOKESTATIC:
                stackSize += doMethodEmulation(((MethodInsnNode) e).desc);
                break;
            case INVOKEDYNAMIC:
                stackSize += doMethodEmulation(((InvokeDynamicInsnNode) e).desc);
                break;
            case JSR:
            case RET:
                throw new RuntimeException("Did not expect JSR/RET instructions");
            default:
                break;
        }
        return stackSize;
    }

    public static InsnList getSafeStringInsnList(String string) {
        InsnList insnList = new InsnList();
        if ((string.length() * 3) < MAX_SAFE_BYTE_COUNT) { // faster check
            insnList.add(new LdcInsnNode(string));
            return insnList;
        }

        insnList.add(new TypeInsnNode(NEW, "java/lang/StringBuilder"));
        insnList.add(new InsnNode(DUP));
        insnList.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));

        String[] chunks = splitUtf8ToChunks(string, MAX_SAFE_BYTE_COUNT);
        for (String chunk : chunks) {
            insnList.add(new LdcInsnNode(chunk));
            insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        }
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));

        return insnList;
    }

    public static String[] splitUtf8ToChunks(String text, int maxBytes) {
        List<String> parts = new ArrayList<>();

        char[] chars = text.toCharArray();

        int lastCharIndex = 0;
        int currentChunkSize = 0;

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            int charSize = getUtf8CharSize(c);
            if (currentChunkSize + charSize < maxBytes) {
                currentChunkSize += charSize;
            } else {
                parts.add(text.substring(lastCharIndex, i));
                currentChunkSize = 0;
                lastCharIndex = i;
            }
        }

        if (currentChunkSize != 0) {
            parts.add(text.substring(lastCharIndex));
        }

        return parts.toArray(new String[0]);
    }

    public static int getUtf8CharSize(char c) {
        if (c >= 0x0001 && c <= 0x007F) {
            return 1;
        } else if (c <= 0x07FF) {
            return 2;
        }
        return 3;
    }

    public static InsnList push(final int value) {
        InsnList ret = new InsnList();
        if (value >= -1 && value <= 5)
            ret.add(new InsnNode(Opcodes.ICONST_0 + value));
        else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE)
            ret.add(new IntInsnNode(Opcodes.BIPUSH, value));
        else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE)
            ret.add(new IntInsnNode(Opcodes.SIPUSH, value));
        else
            ret.add(new LdcInsnNode(value));
        return ret;
    }

    public static InsnList makeValueEnumGetter(@SuppressWarnings("rawtypes") Enum u) {
        InsnList ret = new InsnList();
        Type e = Type.getType(u.getClass());
        ret.add(new FieldInsnNode(Opcodes.GETSTATIC, e.getInternalName(), u.name(), e.getDescriptor()));
        return ret;
    }
}
