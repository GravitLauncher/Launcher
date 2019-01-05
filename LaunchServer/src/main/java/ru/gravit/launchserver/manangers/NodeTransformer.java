package ru.gravit.launchserver.manangers;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launchserver.asm.SafeClassWriter;
import ru.gravit.launchserver.binary.JARLauncherBinary;
import ru.gravit.launchserver.binary.tasks.MainBuildTask;
import ru.gravit.launchserver.manangers.hook.BuildHookManager.Transformer;

public class NodeTransformer implements Transformer {
    @FunctionalInterface
    public interface ClassNodeTransformer {
        void transform(ClassNode node, String classname, JARLauncherBinary data);
    }

    private final List<ClassNodeTransformer> transLst;

    public List<ClassNodeTransformer> getTransLst() {
        return transLst;
    }

    public NodeTransformer() {
        transLst = new ArrayList<>();
    }

    @Override
    public byte[] transform(byte[] input, String classname, MainBuildTask data) {
        //ClassReader cr = new ClassReader(input);
        //ClassNode cn = new ClassNode();
        //cr.accept(cn, data.server.config.env.equals(LauncherConfig.LauncherEnvironment.PROD) || data.server.config.env.equals(LauncherConfig.LauncherEnvironment.STD) ? ClassReader.SKIP_DEBUG : 0);
        //for (ClassNodeTransformer tr : transLst) tr.transform(cn, classname, data);
        //ClassWriter cw = new SafeClassWriter(data.reader, ClassWriter.COMPUTE_MAXS);
        //cn.accept(cw);
        //return cw.toByteArray();
        return null;
    }
}
