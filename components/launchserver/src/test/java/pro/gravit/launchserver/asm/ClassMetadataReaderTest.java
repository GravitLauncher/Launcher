package pro.gravit.launchserver.asm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ClassMetadataReaderTest {
    @Test
    void readsUnresolvedSystemModule() throws Exception {
        String moduleName = "jdk.jlink";
        assumeTrue(ModuleFinder.ofSystem().find(moduleName).isPresent());
        assumeTrue(ModuleLayer.boot().findModule(moduleName).isEmpty());

        try (ClassMetadataReader reader = new ClassMetadataReader()) {
            String className = "jdk/tools/jlink/internal/Main";
            assertEquals(className, new ClassReader(reader.getClassData(className)).getClassName());
            assertEquals("java/lang/Object", reader.getSuperClasses(className).getFirst());
        }
    }

    @Test
    void readsJavaFxHierarchyFromSeparateJars(@TempDir Path javaHome) throws Exception {
        Path lib = Files.createDirectories(javaHome.resolve("lib"));
        writeModuleJar(lib, "javafx.graphics", Map.of("javafx/scene/Node", "java/lang/Object"));
        writeModuleJar(lib, "javafx.controls", Map.of(
                "javafx/scene/control/Control", "javafx/scene/Node",
                "javafx/scene/control/Button", "javafx/scene/control/Control"));

        try (ClassMetadataReader reader = new ClassMetadataReader(new ArrayList<>(), javaHome)) {
            ClassReader button = new ClassReader(reader.getClassData("javafx/scene/control/Button"));
            assertEquals("javafx/scene/control/Control", button.getSuperName());
            assertEquals(List.of("java/lang/Object", "javafx/scene/Node",
                            "javafx/scene/control/Control", "javafx/scene/control/Button"),
                    reader.getSuperClasses(button.getClassName()));
            // Metadata-only modules must not become libraries to attach to the launcher.
            assertTrue(reader.getCp().isEmpty());
            reader.close();
            assertEquals("javafx/scene/control/Control",
                    new ClassReader(reader.getClassData(button.getClassName())).getSuperName());
        }
    }

    private void writeModuleJar(Path directory, String moduleName, Map<String, String> classes) throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Automatic-Module-Name", moduleName);
        try (JarOutputStream output = new JarOutputStream(
                Files.newOutputStream(directory.resolve(moduleName + ".jar")), manifest)) {
            for (var entry : classes.entrySet()) {
                ClassWriter writer = new ClassWriter(0);
                writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, entry.getKey(), null, entry.getValue(), null);
                writer.visitEnd();
                output.putNextEntry(new JarEntry(entry.getKey() + ".class"));
                output.write(writer.toByteArray());
                output.closeEntry();
            }
        }
    }

    @Test
    void canReuseReaderAfterClosingModuleReaders() throws Exception {
        try (ClassMetadataReader reader = new ClassMetadataReader()) {
            assertEquals("java/lang/String", new ClassReader(reader.getClassData("java/lang/String")).getClassName());
            reader.close();
            assertEquals("java/lang/String", new ClassReader(reader.getClassData("java/lang/String")).getClassName());
        }
    }
}
