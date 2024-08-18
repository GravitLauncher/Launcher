package pro.gravit.launchserver.helper;

import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileBuilder;
import pro.gravit.launcher.base.profiles.ClientProfileVersions;
import pro.gravit.launcher.base.profiles.optional.OptionalFile;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalActionFile;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalActionJvmArgs;
import pro.gravit.launcher.base.profiles.optional.triggers.OSTrigger;
import pro.gravit.utils.helper.JVMHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MakeProfileHelper {
    public static ClientProfile makeProfile(ClientProfile.Version version, String title, MakeProfileOption... options) {
        ClientProfileBuilder builder = new ClientProfileBuilder();
        builder.setVersion(version);
        builder.setDir(title);
        builder.setAssetDir("assets");
        builder.setAssetIndex(version.toString());
        builder.setInfo("Server description");
        builder.setTitle(title);
        builder.setUuid(UUID.randomUUID());
        builder.setMainClass(getMainClassByVersion(version, options));
        builder.setServers(List.of(new ClientProfile.ServerProfile(title, "localhost", 25565)));
        // ------------
        var lwjgl3ify = findOption(options, Lwjgl3ifyOption.class);
        builder.setUpdateVerify(List.of("libraries", "natives", "mods", "minecraft.jar", "forge.jar", "liteloader.jar"));
        {
            List<String> classPath = new ArrayList<>(5);
            if(findOption(options, MakeProfileOptionLaunchWrapper.class).isPresent()) {
                classPath.add("libraries/net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar");
            }
            classPath.add("libraries");
            classPath.add("minecraft.jar");
            if (version.compareTo(ClientProfileVersions.MINECRAFT_1_12_2) <= 0) {
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
        if (version.compareTo(ClientProfileVersions.MINECRAFT_1_12_2) <= 0) {
            // lwjgl3ify arguments https://github.com/GTNewHorizons/lwjgl3ify
            jvmArgs.add("-Djava.security.manager=allow");
            jvmArgs.add("--add-opens");
            jvmArgs.add("java.base/jdk.internal.loader=ALL-UNNAMED");
            jvmArgs.add("--add-opens");
            jvmArgs.add("java.base/java.net=ALL-UNNAMED");
            jvmArgs.add("--add-opens");
            jvmArgs.add("java.base/java.nio=ALL-UNNAMED");
            jvmArgs.add("--add-opens");
            jvmArgs.add("java.base/java.io=ALL-UNNAMED");
            jvmArgs.add("--add-opens");
            jvmArgs.add("java.base/java.lang=ALL-UNNAMED");
            jvmArgs.add("--add-opens");
            jvmArgs.add("java.base/java.lang.reflect=ALL-UNNAMED");
            jvmArgs.add("--add-opens");
            jvmArgs.add("java.base/java.text=ALL-UNNAMED");
            jvmArgs.add("--add-opens");
            jvmArgs.add("java.base/java.util=ALL-UNNAMED");
            jvmArgs.add("--add-opens");
            jvmArgs.add("java.base/jdk.internal.reflect=ALL-UNNAMED");
            jvmArgs.add("--add-opens");
            jvmArgs.add("java.base/sun.nio.ch=ALL-UNNAMED");
            jvmArgs.add("--add-opens");
            jvmArgs.add("jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED,java.naming");
            jvmArgs.add("--add-opens");
            jvmArgs.add("java.desktop/sun.awt=ALL-UNNAMED");
            jvmArgs.add("--add-opens");
            jvmArgs.add("java.desktop/sun.awt.image=ALL-UNNAMED");
            jvmArgs.add("--add-opens");
            jvmArgs.add("java.desktop/com.sun.imageio.plugins.png=ALL-UNNAMED");
            jvmArgs.add("--add-opens");
            jvmArgs.add("jdk.dynalink/jdk.dynalink.beans=ALL-UNNAMED");
            jvmArgs.add("--add-opens");
            jvmArgs.add("java.sql.rowset/javax.sql.rowset.serial=ALL-UNNAMED");
        } else if (version.compareTo(ClientProfileVersions.MINECRAFT_1_18) <= 0) { // 1.13 - 1.16.5
            jvmArgs.add("-XX:+UseG1GC");
            jvmArgs.add("-XX:+UnlockExperimentalVMOptions");
        } else {
        }
        // -----------
        Optional<MakeProfileOptionForge> forge = findOption(options, MakeProfileOptionForge.class);
        Optional<MakeProfileOptionFabric> fabric = findOption(options, MakeProfileOptionFabric.class);
        Optional<MakeProfilesOptionsQuilt> quilt = findOption(options, MakeProfilesOptionsQuilt.class);
        if (version.compareTo(ClientProfileVersions.MINECRAFT_1_12_2) > 0) {
            jvmArgs.add("-Djava.library.path=natives");
            OptionalFile optionalMacOs = new OptionalFile();
            optionalMacOs.name = "MacOSArgs";
            optionalMacOs.visible = false;
            optionalMacOs.actions = new ArrayList<>(1);
            optionalMacOs.actions.add(new OptionalActionJvmArgs(List.of("-XstartOnFirstThread")));
            optionalMacOs.triggersList = List.of(new OSTrigger(JVMHelper.OS.MACOSX));
            optionals.add(optionalMacOs);
        }
        if (fabric.isPresent()) {
            builder.setAltClassPath(fabric.orElseThrow().getAltClassPath());
            jvmArgs.add("-Dsodium.checks.issue2561=false"); // Please don't check LWJL3 version (Sodium: https://github.com/CaffeineMC/sodium-fabric/issues/2561 )
        }
        if(quilt.isPresent()) {
            builder.setClassLoaderConfig(ClientProfile.ClassLoaderConfig.SYSTEM_ARGS);
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
        Optional<MakeProfileOptionLog4j> logFile = findOption(options, MakeProfileOptionLog4j.class);
        if (logFile.isPresent()) {
            var log4jOption = logFile.get();
            if (log4jOption.logFile != null) {
                jvmArgs.add("-Dlog4j.configurationFile=".concat(logFile.get().logFile));
            } else if (log4jOption.affected) {
                if (version.compareTo(ClientProfileVersions.MINECRAFT_1_17) >= 0 && version.compareTo(ClientProfileVersions.MINECRAFT_1_18) < 0) {
                    jvmArgs.add("-Dlog4j2.formatMsgNoLookups=true");
                }
            }
        }
        builder.setMinJavaVersion(17);
        builder.setRecommendJavaVersion(17);
        if(version.compareTo(ClientProfileVersions.MINECRAFT_1_20_3) >= 0) {
            builder.setMinJavaVersion(21);
            builder.setRecommendJavaVersion(21);
        }
        jvmArgs.add("-Dfml.ignorePatchDiscrepancies=true");
        jvmArgs.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
        builder.setJvmArgs(jvmArgs);
        builder.setUpdateOptional(optionals);
        List<String> clientArgs = new ArrayList<>();
        if(version.compareTo(ClientProfileVersions.MINECRAFT_1_7_10) == 0) {
            jvmArgs.add("-Drfb.skipClassLoaderCheck=true");
        }
        if (findOption(options, MakeProfileOptionLaunchWrapper.class).isPresent() || findOption(options, MakeProfileRfbOption.class).isPresent()) {
            if (findOption(options, MakeProfileOptionLiteLoader.class).isPresent()) {
                clientArgs.add("--tweakClass");
                clientArgs.add("com.mumfrey.liteloader.launch.LiteLoaderTweaker");
            }
            if (forge.isPresent()) {
                clientArgs.add("--tweakClass");
                if (version.compareTo(ClientProfileVersions.MINECRAFT_1_7_10) > 0) {
                    clientArgs.add("net.minecraftforge.fml.common.launcher.FMLTweaker");
                } else {
                    clientArgs.add("cpw.mods.fml.common.launcher.FMLTweaker");
                }
            }
        } else if (version.compareTo(ClientProfileVersions.MINECRAFT_1_12_2) > 0) {
            forge.ifPresent(makeProfileOptionForge -> clientArgs.addAll(makeProfileOptionForge.makeClientArgs()));
        }
        builder.setClientArgs(clientArgs);

        return builder.createClientProfile();
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<T> findOption(MakeProfileOption[] options, Class<T> clazz) {
        return (Optional<T>) Arrays.stream(options).filter((o) -> clazz.isAssignableFrom(o.getClass())).findFirst();
    }

    public static String getMainClassByVersion(ClientProfile.Version version, MakeProfileOption... options) {
        if(version.compareTo(ClientProfileVersions.MINECRAFT_1_7_10) == 0) {
            return  "com.gtnewhorizons.retrofuturabootstrap.Main";
        }
        if (findOption(options, MakeProfileOptionLaunchWrapper.class).isPresent()) {
            return "net.minecraft.launchwrapper.Launch";
        }
        if (findOption(options, MakeProfileOptionForge.class).isPresent() && version.compareTo(ClientProfileVersions.MINECRAFT_1_12_2) > 0) {
            return "cpw.mods.modlauncher.Launcher";
        }
        if (findOption(options, MakeProfileOptionFabric.class).isPresent()) {
            return "net.fabricmc.loader.impl.launch.knot.KnotClient";
        }
        if(findOption(options, MakeProfilesOptionsQuilt.class).isPresent()) {
            return "org.quiltmc.loader.impl.launch.knot.KnotClient";
        }
        return "net.minecraft.client.main.Main";
    }

    private static boolean isAffectedLog4jVersion(String version) {
        if (version == null) {
            return true;
        }
        String[] split = version.split("\\.");
        if (split.length < 2) return true;
        if (!split[0].equals("2")) return false;
        return Integer.parseInt(split[1]) < 15;
    }

    private static String getLog4jVersion(Path dir) throws IOException {
        Path log4jCore = dir.resolve("org/apache/logging/log4j/log4j-core");
        if (Files.exists(log4jCore)) {
            Path target = Files.list(log4jCore).findFirst().orElse(null);
            if (target != null) {
                return target.getFileName().toString();
            }
        }
        return null;
    }

    public static MakeProfileOption[] getMakeProfileOptionsFromDir(Path dir, ClientProfile.Version version) throws IOException {
        List<MakeProfileOption> options = new ArrayList<>(2);
        if (Files.exists(dir.resolve("forge.jar"))) {
            options.add(new MakeProfileOptionForge());
        } else if (Files.exists(dir.resolve("libraries/net/minecraftforge/forge"))) {
            if (version.compareTo(ClientProfileVersions.MINECRAFT_1_12_2) > 0) {
                options.add(new MakeProfileOptionForge(dir));
            } else {
                options.add(new MakeProfileOptionForge());
            }
        }
        if (Files.exists(dir.resolve("libraries/net/fabricmc/fabric-loader"))) {
            options.add(new MakeProfileOptionFabric(dir));
        }
        if (Files.exists(dir.resolve("libraries/org/quiltmc/quilt-loader"))) {
            options.add(new MakeProfilesOptionsQuilt());
        }
        {
            String log4jVersion = getLog4jVersion(dir);
            if (log4jVersion != null) {

                boolean affected = isAffectedLog4jVersion(log4jVersion);
                if (Files.exists(dir.resolve("log4j2_custom.xml"))) {
                    options.add(new MakeProfileOptionLog4j(affected, "log4j2_custom.xml"));
                } else {
                    options.add(new MakeProfileOptionLog4j(affected, null));
                }
            }
        }
        if (Files.exists(dir.resolve("liteloader.jar"))) {
            options.add(new MakeProfileOptionLiteLoader());
        }
        if (Files.exists(dir.resolve("libraries/org/lwjgl/lwjgl/3.2.2")) && Files.exists(dir.resolve("libraries/org/lwjgl/lwjgl/3.2.1"))) {
            options.add(new MakeProfileOptionLwjgl());
        }
        if (Files.exists(dir.resolve("libraries/forge/launchwrapper-1.12-launcherfixed.jar.jar")) || Files.exists(dir.resolve("libraries/net/minecraft/launchwrapper"))) {
            options.add(new MakeProfileOptionLaunchWrapper());
        }
        return options.toArray(new MakeProfileOption[0]);
    }

    private static Path findFirstDir(Path path) throws IOException {
        return Files.list(path).findFirst().orElse(null);
    }

    private static Path findFirstMavenFile(Path path) throws IOException {
        return Files.list(Files.list(path).findFirst().orElseThrow()).filter(e -> e.getFileName().toString().endsWith(".jar")).findFirst().orElseThrow();
    }

    public interface MakeProfileOption {
    }

    public static class MakeProfileOptionLog4j implements MakeProfileOption {
        public boolean affected;
        public String logFile;

        public MakeProfileOptionLog4j(boolean lower15, String logFile) {
            this.affected = lower15;
            this.logFile = logFile;
        }
    }

    public static class MakeProfileOptionForge implements MakeProfileOption {
        public String launchTarget;
        public String forgeVersion;
        public String forgeGroup;
        public String minecraftVersion;
        public String mcpVersion;

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

        public List<String> makeClientArgs() {
            if (launchTarget == null) return List.of();
            return List.of("--launchTarget", launchTarget, "--fml.forgeVersion", forgeVersion, "--fml.mcVersion", minecraftVersion, "--fml.forgeGroup", forgeGroup, "--fml.mcpVersion", mcpVersion);
        }
    }

    public static class MakeProfileOptionLaunchWrapper implements MakeProfileOption {

    }

    public static class MakeProfileRfbOption implements MakeProfileOption {

    }

    public record Lwjgl3ifyOption(String lwjgl3ifyForgePatches) implements MakeProfileOption {
    }

    public static class MakeProfileOptionFabric implements MakeProfileOption {
        public String jimfsPath;
        public String guavaPath;

        public MakeProfileOptionFabric() {
        }

        public MakeProfileOptionFabric(String jimfsPath, String guavaPath) {
            this.jimfsPath = jimfsPath;
            this.guavaPath = guavaPath;
        }

        public MakeProfileOptionFabric(Path clientDir) throws IOException {
            if (Files.exists(clientDir.resolve("libraries/com/google/jimfs/jimfs"))) {
                jimfsPath = clientDir.relativize(findFirstMavenFile(clientDir.resolve("libraries/com/google/jimfs/jimfs"))).toString();
                guavaPath = clientDir.relativize(findFirstMavenFile(clientDir.resolve("libraries/com/google/guava/guava/"))).toString();
            }
        }

        public List<String> getAltClassPath() {
            if (jimfsPath == null || guavaPath == null) return List.of();
            return List.of(jimfsPath, guavaPath);
        }
    }

    public static class MakeProfilesOptionsQuilt implements MakeProfileOption {

    }

    public static class MakeProfileOptionLiteLoader implements MakeProfileOption {

    }

    public static class MakeProfileOptionLwjgl implements MakeProfileOption {

    }
}
