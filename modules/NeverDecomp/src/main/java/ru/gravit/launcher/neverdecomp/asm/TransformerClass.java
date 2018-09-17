package ru.gravit.launcher.neverdecomp.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import ru.gravit.launchserver.manangers.BuildHookManager.Transformer;

public class TransformerClass implements Transformer {
	
	private final boolean context;

	public TransformerClass(boolean hobf) {
		this.context = hobf;
	}

	@Override
	public byte[] transform(byte[] input, CharSequence classname) {
		ClassReader classReader = new ClassReader(input);
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classReader.accept(new AntiDecompileClassVisitor(writer, context), 0);
		return writer.toByteArray();
	}
}