package ru.gravit.launcher.neverdecomp.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class AntiDecompileMethodVisitor extends AdviceAdapter implements Opcodes {

	private final boolean context;

	protected AntiDecompileMethodVisitor(int access, MethodVisitor mw, String name, String desc, boolean context) {
		super(ASM5, mw, access, name, desc);
		this.context = context;
	}

	// в начале каждого метода
	// убивает декомпиляторы
	@Override
	public void onMethodEnter() {
		if (context) expAntiDecomp();
		antiDecomp();		
	}
	
	public void expAntiDecomp() {
		Label lbl1 = this.newLabel(), lbl15 = this.newLabel(), 
				lbl2 = this.newLabel(), lbl25 = this.newLabel();
		
		// try-catch блок с lbl1 до lbl2 с переходом на lbl2 при java/lang/Exception
		this.visitException(lbl1, lbl2, lbl2);
		
		// lbl1: iconst_0
		this.visitLabel(lbl1);
		this.visitInsn(ICONST_0);
		// lbl15: pop; goto lbl25 
		this.visitLabel(lbl15);
		this.visitInsn(POP);
		this.jumpLabel(lbl25);
		// lbl2: pop; pop2
		this.visitLabel(lbl2);
		this.visitInsn(POP);
		this.visitInsn(POP);
		// lbl25:
		this.visitLabel(lbl25);
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
		this.visitTryCatchBlock(st, en, h, "java/lang/Exception");
	}

	public void jumpLabel(Label to) {
		this.visitJumpInsn(GOTO, to);
	}
}