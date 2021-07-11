package pro.gravit.launchserver.helper;

import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.ClientProfileBuilder;
import pro.gravit.launcher.profiles.optional.OptionalFile;
import pro.gravit.launcher.profiles.optional.actions.OptionalActionFile;
import pro.gravit.launcher.profiles.optional.actions.OptionalActionJvmArgs;
import pro.gravit.launcher.profiles.optional.triggers.OSTrigger;
import pro.gravit.utils.helper.JVMHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MakeProfileHelper {
    public static ClientProfile makeProfile(ClientProfile.Version version, String title, MakeProfileOption... options) {
        ClientProfileBuilder builder = new ClientProfileBuilder();
        builder.setVersion(version.name);
        builder.setDir(title);
        builder.setAssetDir("asset" + version.name);
        builder.setAssetIndex(version.name);
        builder.setInfo("Информация о сервере");
        builder.setTitle(title);
        builder.setUuid(UUID.randomUUID());
        builder.setMainClass(getMainClassByVersion(version, options));
        builder.setServers(List.of(new ClientProfile.ServerProfile(title, "localhost", 25535)));
        // ------------
        builder.setUpdateVerify(List.of("libraries", "natives", "mods", "minecraft.jar", "forge.jar", "liteloader.jar"));
        {
            List<String> classPath = new ArrayList<>(5);
            classPath.add("libraries");
            classPath.add("minecraft.jar");
            if (version.compareTo(ClientProfile.Version.MC1122) <= 0) {
                findOption(options, MakeProfileOptionForge.class).ifPresent(e -> classPath.add("forge.jar"));
                findOption(options, MakeProfileOptionLiteLoader.class).ifPresent(e -> classPath.add("liteloader.jar"));
            }
            builder.setClassPath(classPath);
        }
        builder.setUpdate(List.of("servers.dat"));
        List<String> jvmArgs = new ArrayList<>(4);
        Set<OptionalFile> optionals = new HashSet<>();
        jvmArgs.add("-XX:+DisableAttachMechanism");
        // Official Mojang launcher java arguments
        jvmArgs.add("-XX:+UseG1GC");
        jvmArgs.add("-XX:+UnlockExperimentalVMOptions");
        jvmArgs.add("-XX:G1NewSizePercent=20");
        jvmArgs.add("-XX:MaxGCPauseMillis=50");
        jvmArgs.add("-XX:G1HeapRegionSize=32M");
        // -----------
        Optional<MakeProfileOptionForge> forge = findOption(options, MakeProfileOptionForge.class);
        if (version.compareTo(ClientProfile.Version.MC1122) > 0) {
            jvmArgs.add("-Djava.library.path=natives");
            OptionalFile optionalMacOs = new OptionalFile();
            optionalMacOs.name = "MacOSArgs";
            optionalMacOs.visible = false;
            optionalMacOs.actions = new ArrayList<>(1);
            optionalMacOs.actions.add(new OptionalActionJvmArgs(List.of("-XstartOnFirstThread")));
            optionalMacOs.triggersList = List.of(new OSTrigger(JVMHelper.OS.MACOSX));
            optionals.add(optionalMacOs);
        }
        if (findOption(options, MakeProfileOptionLwjgl.class).isPresent()) {
            OptionalFile optionalMac = new OptionalFile();
            optionalMac.name = "MacLwjgl";
            optionalMac.visible = false;
            optionalMac.actions = new ArrayList<>(1);
            optionalMac.actions.add(new OptionalActionFile(Map.of(
                    "libraries/org/lwjgl/lwjgl/3.2.1", "",
                    "libraries/org/lwjgl/lwjgl-glfw/3.2.1", "",
                    "libraries/org/lwjgl/lwjgl-openal/3.2.1", "",
                    "libraries/org/lwjgl/lwjgl-stb/3.2.1", "",
                    "libraries/org/lwjgl/lwjgl-tinyfd/3.2.1", "",
                    "libraries/org/lwjgl/lwjgl-opengl/3.2.1", "",
                    "libraries/org/lwjgl/lwjgl-jemalloc/3.2.1", ""
            )));
            optionalMac.triggersList = List.of(new OSTrigger(JVMHelper.OS.MACOSX));
            optionals.add(optionalMac);
            OptionalFile optionalOther = new OptionalFile();
            optionalOther.name = "NonMacLwjgl";
            optionalOther.visible = false;
            optionalOther.actions = new ArrayList<>(1);
            optionalOther.actions.add(new OptionalActionFile(Map.of(
                    "libraries/org/lwjgl/lwjgl/3.2.2", "",
                    "libraries/org/lwjgl/lwjgl-glfw/3.2.2", "",
                    "libraries/org/lwjgl/lwjgl-openal/3.2.2", "",
                    "libraries/org/lwjgl/lwjgl-stb/3.2.2", "",
                    "libraries/org/lwjgl/lwjgl-tinyfd/3.2.2", "",
                    "libraries/org/lwjgl/lwjgl-opengl/3.2.2", "",
                    "libraries/org/lwjgl/lwjgl-jemalloc/3.2.2", ""
            )));
            OSTrigger nonMacTrigger = new OSTrigger(JVMHelper.OS.MACOSX);
            nonMacTrigger.inverted = true;
            optionalOther.triggersList = List.of(nonMacTrigger);
            optionals.add(optionalOther);
        }
        if (version.compareTo(ClientProfile.Version.MC117) >= 0) {
            builder.setMinJavaVersion(16);
            builder.setRecommendJavaVersion(16);
        }
        jvmArgs.add("-Dfml.ignorePatchDiscrepancies=true");
        jvmArgs.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
        builder.setJvmArgs(jvmArgs);
        builder.setUpdateOptional(optionals);
        List<String> clientArgs = new ArrayList<>();
        if (findOption(options, MakeProfileOptionLaunchWrapper.class).isPresent()) {
            if (findOption(options, MakeProfileOptionLiteLoader.class).isPresent()) {
                clientArgs.add("--tweakClass");
                clientArgs.add("com.mumfrey.liteloader.launch.LiteLoaderTweaker");
            }
            if (forge.isPresent()) {
                clientArgs.add("--tweakClass");
                if (version.compareTo(ClientProfile.Version.MC1710) > 0) {
                    clientArgs.add("net.minecraftforge.fml.common.launcher.FMLTweaker");
                } else {
                    clientArgs.add("cpw.mods.fml.common.launcher.FMLTweaker");
                }
                if (version.compareTo(ClientProfile.Version.MC1122) <= 0) {
                    builder.setMinJavaVersion(8);
                    builder.setRecommendJavaVersion(8);
                    builder.setMaxJavaVersion(8);
                }
            }
        } else if (version.compareTo(ClientProfile.Version.MC1122) > 0) {
            if (forge.isPresent()) {
                clientArgs.addAll(forge.get().makeClientArgs());
                builder.setClassLoaderConfig(ClientProfile.ClassLoaderConfig.AGENT);
                if (version.compareTo(ClientProfile.Version.MC1165) <= 0) {
                    builder.setMaxJavaVersion(15);
                }
            }
        }
        builder.setClientArgs(clientArgs);

        return builder.createClientProfile();
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<T> findOption(MakeProfileOption[] options, Class<T> clazz) {
        return (Optional<T>) Arrays.stream(options).filter((o) -> clazz.isAssignableFrom(o.getClass())).findFirst();
    }

    public static String getMainClassByVersion(ClientProfile.Version version, MakeProfileOption... options) {
        if (findOption(options, MakeProfileOptionLaunchWrapper.class).isPresent()) {
            return "net.minecraft.launchwrapper.Launch";
        }
        if (findOption(options, MakeProfileOptionForge.class).isPresent() && version.compareTo(ClientProfile.Version.MC1122) > 0) {
            return "cpw.mods.modlauncher.Launcher";
        }
        if (findOption(options, MakeProfileOptionFabric.class).isPresent()) {
            return "net.fabricmc.loader.launch.knot.KnotClient";
        }
        return "net.minecraft.client.main.Main";
    }

    public static MakeProfileOption[] getMakeProfileOptionsFromDir(Path dir, ClientProfile.Version version) throws IOException {
        List<MakeProfileOption> options = new ArrayList<>(2);
        if (version.compareTo(ClientProfile.Version.MC1122) <= 0) {
            if (Files.exists(dir.resolve("forge.jar"))) {
                options.add(new MakeProfileOptionForge());
            }
        } else {
            if (Files.exists(dir.resolve("libraries/net/minecraftforge/forge"))) {
                options.add(new MakeProfileOptionForge(dir));
            }
            if (Files.exists(dir.resolve("libraries/net/fabricmc/fabric-loader"))) {
                options.add(new MakeProfileOptionFabric());
            }
        }
        if (Files.exists(dir.resolve("liteloader.jar"))) {
            options.add(new MakeProfileOptionLiteLoader());
        }
        if (Files.exists(dir.resolve("libraries/org/lwjgl/lwjgl/3.2.2")) && Files.exists(dir.resolve("libraries/org/lwjgl/lwjgl/3.2.1"))) {
            options.add(new MakeProfileOptionLwjgl());
        }
        if (version.compareTo(ClientProfile.Version.MC1122) <= 0) {
            options.add(new MakeProfileOptionLaunchWrapper());
        }
        return options.toArray(new MakeProfileOption[0]);
    }

    public interface MakeProfileOption {
    }

    public static class MakeProfileOptionForge implements MakeProfileOption {
        public String launchTarget;
        public String forgeVersion;
        public String forgeGroup;
        public String minecraftVersion;
        public String mcpVersion;

        public List<String> makeClientArgs() {
            if (launchTarget == null) return List.of();
            return List.of("--launchTarget", launchTarget, "--fml.forgeVersion", forgeVersion, "--fml.mcVersion", minecraftVersion, "--fml.forgeGroup", forgeGroup, "--fml.mcpVersion", mcpVersion);
        }

        public MakeProfileOptionForge() {

        }

        public MakeProfileOptionForge(String launchTarget, String forgeVersion, String forgeGroup, String minecraftVersion, String mcpVersion) {
            this.launchTarget = launchTarget;
            this.forgeVersion = forgeVersion;
            this.forgeGroup = forgeGroup;
            this.minecraftVersion = minecraftVersion;
            this.mcpVersion = mcpVersion;
        }

        public MakeProfileOptionForge(Path clientDir) throws IOException {
            Path libraries = clientDir.resolve("libraries");
            if (!Files.exists(libraries)) {
                throw new IOException("libraries not found");
            }
            Path forgePath = findFirstDir(libraries.resolve("net/minecraftforge/forge"));
            if (forgePath == null) {
                throw new IOException("forge not found");
            }
            String[] forgeFullVersion = forgePath.getFileName().toString().split("-");
            minecraftVersion = forgeFullVersion[0];
            forgeVersion = forgeFullVersion[1];
            launchTarget = "fmlclient";
            forgeGroup = "net.minecraftforge";
            Path minecraftPath = findFirstDir(libraries.resolve("net/minecraft/client"));
            if (minecraftPath == null) {
                throw new IOException("mcp not found");
            }
            String[] minecraftFullVersion = minecraftPath.getFileName().toString().split("-");
            mcpVersion = minecraftFullVersion[1];
        }
    }

    private static Path findFirstDir(Path path) throws IOException {
        return Files.list(path).findFirst().orElse(null);
    }

    public static class MakeProfileOptionLaunchWrapper implements MakeProfileOption {

    }

    public static class MakeProfileOptionFabric implements MakeProfileOption {

    }

    public static class MakeProfileOptionLiteLoader implements MakeProfileOption {

    }

    public static class MakeProfileOptionLwjgl implements MakeProfileOption {

    }
}
