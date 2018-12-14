package ru.gravit.launchserver.asm;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.util.Random;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public class AntiDecomp {
	private static final Class<?>[] exceptionsL = {
			Throwable.class, Exception.class, Error.class, InternalError.class, RuntimeException.class, NullPointerException.class, 
			AssertionError.class, NoClassDefFoundError.class, IOException.class, NoSuchFieldException.class, SecurityException.class, InvocationTargetException.class
		};
	private static class AObfClassVisitor extends ClassVisitor {
		private Random r = new SecureRandom();
		private AObfClassVisitor(ClassVisitor classVisitor) {
			super(Opcodes.ASM7, classVisitor);
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			return new AdviceAdapter(Opcodes.ASM7, super.visitMethod(access, name, desc, signature, exceptions), access,
					name, desc) {
				
				@Override
				public void onMethodEnter() {
					antiDecomp();
				}

				public void antiDecomp() {
					Label lbl1 = this.newLabel(), lbl15 = this.newLabel(), 
							lbl2 = this.newLabel(), lbl3 = this.newLabel(), 
							lbl35 = this.newLabel(), lbl4 = this.newLabel();
					
					// try-catch блок с lbl1 до lbl2 с переходом на lbl15 при java/lang/Exception
					this.visitException(lbl1, lbl2, lbl15);
					// try-catch блок с lbl3 до lbl4 с переходом на lbl3 при java/lang/Exception
					this.visitException(lbl3, lbl4, lbl3);
					
					// lbl1: goto lbl2
					this.visitLabel(lbl1);
					this.jumpLabel(lbl2);
					// lbl15: pop
					this.visitLabel(lbl15);
					this.visitInsn(POP);
					// lbl2: goto lbl35
					this.visitLabel(lbl2);
					this.jumpLabel(lbl35);
					// lbl3: pop
					this.visitLabel(lbl3);
					this.visitInsn(POP);
					// lbl35: nop
					this.visitLabel(lbl35);
					this.visitInsn(NOP);
					// lbl4: nop
					this.visitLabel(lbl4);
					this.visitInsn(NOP);
				}

				public void visitException(Label st, Label en, Label h) {
					super.visitTryCatchBlock(st, en, h, Type.getInternalName(exceptionsL[r.nextInt(exceptionsL.length-1)]));
				}

				public void jumpLabel(Label to) {
					super.visitJumpInsn(GOTO, to);
				}
			};
		}
	}
	
	private AntiDecomp() {
	}

	public static byte[] antiDecomp(final byte[] input, ClassMetadataReader reader) {
		ClassReader cr = new ClassReader(input);
		ClassWriter cw = new SafeClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cr.accept(new AObfClassVisitor(cw), ClassReader.SKIP_DEBUG | ClassReader.EXPAND_FRAMES);
		return cw.toByteArray();
	}
}
