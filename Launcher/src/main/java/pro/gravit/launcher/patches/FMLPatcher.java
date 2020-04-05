package pro.gravit.launcher.patches;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import pro.gravit.utils.helper.SecurityHelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Random;
import java.util.Vector;

public class FMLPatcher extends ClassLoader implements Opcodes {
    public static final MethodType EXITMH = MethodType.methodType(void.class, int.class);
    public static final String[] PACKAGES = new String[]{"cpw.mods.fml.", "net.minecraftforge.fml.", "cpw.mods."};
    public static final Vector<MethodHandle> MHS = new Vector<>();
    public static volatile FMLPatcher INSTANCE = null;

    public FMLPatcher(final ClassLoader cl) {
        super(cl);
    }

    public static void apply() {
        INSTANCE = new FMLPatcher(null); // Never cause ClassFormatError (fuck forge 1.14!!!)
        for (String s : PACKAGES) {
            String rMethod = randomStr(16);
            try {
                MHS.add(MethodHandles.publicLookup().findStatic(INSTANCE.def(s + randomStr(16), rMethod), rMethod,
                        EXITMH));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                // Simple ignore - other Forge
            }
        }
    }

    public static void exit(final int code) {
        for (MethodHandle mh : MHS)
            try {
                mh.invoke(code);
            } catch (Throwable ignored) {
            }
    }

    private static byte[] gen(final String name, final String exName) { // "cpw/mods/fml/SafeExitJVMLegacy", "exit"

        final ClassWriter classWriter = new ClassWriter(0);
        MethodVisitor methodVisitor;

        classWriter.visit(V1_8, ACC_PUBLIC | ACC_SUPER, name, null, "java/lang/Object", null);

        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, exName, "(I)V", null, null);
            methodVisitor.visitCode();
            final Label label0 = new Label();
            final Label label1 = new Label();
            final Label label2 = new Label();
            methodVisitor.visitTryCatchBlock(label0, label1, label2, "java/lang/Throwable");
            methodVisitor.visitLabel(label0);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Runtime", "getRuntime", "()Ljava/lang/Runtime;",
                    false);
            methodVisitor.visitVarInsn(ILOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Runtime", "halt", "(I)V", false);
            methodVisitor.visitLabel(label1);
            final Label label3 = new Label();
            methodVisitor.visitJumpInsn(GOTO, label3);
            methodVisitor.visitLabel(label2);
            methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/Throwable"});
            methodVisitor.visitVarInsn(ASTORE, 1);
            methodVisitor.visitVarInsn(ILOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/System", "exit", "(I)V", false);
            methodVisitor.visitLabel(label3);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(2, 2);
            methodVisitor.visitEnd();
        }
        classWriter.visitEnd();
        return classWriter.toByteArray();
    }

    public static String randomStr(final int lenght) {
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        alphabet += alphabet.toUpperCase(Locale.US);
        final StringBuilder sb = new StringBuilder(lenght);
        final Random random = SecurityHelper.newRandom();
        for (int i = 0; i < lenght; i++)
            sb.append(alphabet.charAt(random.nextInt(26)));
        return sb.toString();
    }

    public Class<?> def(final String name, final String exName) {
        return super.defineClass(name, ByteBuffer.wrap(gen(name.replace('.', '/'), exName)), null);
    }
}
