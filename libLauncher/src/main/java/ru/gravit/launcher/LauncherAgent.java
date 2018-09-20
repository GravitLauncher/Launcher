package ru.gravit.launcher;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.jar.JarFile;

import javassist.bytecode.ClassFile;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.launcher.transformers.SystemClassLoaderTransformer;

@LauncherAPI
public class LauncherAgent {
	private static final boolean enabled = false;
	private static boolean isAgentStarted=false;
    public static Instrumentation inst;
    
    public static void addJVMClassPath(String path) throws IOException {
        LogHelper.debug("Launcher Agent addJVMClassPath");
        inst.appendToSystemClassLoaderSearch(new JarFile(path));
    }
	public boolean isAgentStarted()
	{
		return isAgentStarted;
	}
    public static long getObjSize(Object obj) {
    	return inst.getObjectSize(obj);
    }
    
    public static void premain(String agentArgument, Instrumentation instrumentation) {
        System.out.println("Launcher Agent");
        inst = instrumentation;
        isAgentStarted = true;

        if(ClassFile.MAJOR_VERSION > ClassFile.JAVA_8 || enabled) {
	        	inst.addTransformer(new SystemClassLoaderTransformer());
	        Class<?>[] classes = inst.getAllLoadedClasses(); // Получаем список уже загруженных классов, которые могут быть изменены. Классы, которые ещё не загружены, будут изменены при загрузке
	        ArrayList<Class<?>> classList = new ArrayList<>();
	        for (Class<?> classe : classes)
				if (inst.isModifiableClass(classe))
					classList.add(classe);
	        // Reload classes, if possible.
	        Class<?>[] workaround = new Class[classList.size()];
	        try {
	        	inst.retransformClasses(classList.toArray(workaround)); // Запускаем процесс трансформации
	        } catch (UnmodifiableClassException e) {
	        	System.err.println("MainClass was unable to retransform early loaded classes: " + e);
	        }
        }
    }
}
