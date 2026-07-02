package pro.gravit.utils.launch;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class ModuleHacks {

    public static ModuleLayer.Controller createController(MethodHandles.Lookup lookup, ModuleLayer layer) {
        try {
            return (ModuleLayer.Controller) lookup.findConstructor(ModuleLayer.Controller.class, MethodType.methodType(void.class, ModuleLayer.class)).invoke(layer);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
