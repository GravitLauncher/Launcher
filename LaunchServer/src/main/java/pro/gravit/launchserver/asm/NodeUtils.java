package pro.gravit.launchserver.asm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import pro.gravit.utils.helper.IOHelper;

import static org.objectweb.asm.Opcodes.*;

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
		List<AnnotationNode> ret = new ArrayList<>();
		ClassNode n = forClass(clazz, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG, r);
		ret.addAll(n.visibleAnnotations);
		ret.addAll(n.invisibleAnnotations);
		for (MethodNode m : n.methods)
			if (method.equals(m.name)) {
				ret.addAll(n.visibleAnnotations);
				ret.addAll(n.invisibleAnnotations);
			}
		return ret;
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
}
