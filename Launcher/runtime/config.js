// ====== LAUNCHER CONFIG ====== //
var config = {
    dir: "GravitLauncher", // Launcher directory
    title: "GravitLauncher", // Window title
    icons: [ "favicon.png" ], // Window icon paths

    // Auth config
    linkText: "GravitLauncher", // Text for link under "Auth" button
    linkURL: new java.net.URL("https://gravitlauncher.ml"), // URL for link under "Auth" button

    // Menu config
    discord: new java.net.URL("https://discord.gg/aJK6nMN"),

    // Settings defaults
    settingsMagic: 0xC0DE5, // Magic, don't touch
    autoEnterDefault: false, // Should autoEnter be enabled by default?
    fullScreenDefault: false, // Should fullScreen be enabled by default?
    ramDefault: 1024, // Default RAM amount (0 for auto)

    jvm: {
        enable: false,
        jvmMustdie32Dir: "jre-8u202-win32",
        jvmMustdie64Dir: "jre-8u202-win64",
    }
};

if(config.jvm.enable) {
    switch (JVMHelper.OS_TYPE) {
        case JVMHelperOS.MUSTDIE:
            jvmDirName = JVMHelper.OS_BITS === 32 ? config.jvm.jvmMustdie32Dir : 
                jvmDirName = JVMHelper.OS_BITS === 64 ? config.jvm.jvmMustdie64Dir : config.jvm.enable = false;
            break;
        default:
            config.jvm.enable = false;
            break;
    }
}

DirBridge.dir = DirBridge.getLauncherDir(config.dir);
DirBridge.dirStore = DirBridge.getStoreDir(config.dir);
DirBridge.dirProjectStore = DirBridge.getProjectStoreDir(config.dir);
if (!IOHelper.isDir(DirBridge.dir)) {
    java.nio.file.Files.createDirectory(DirBridge.dir);
}
DirBridge.defaultUpdatesDir = DirBridge.dir.resolve("updates");
if (!IOHelper.isDir(DirBridge.defaultUpdatesDir)) {
    java.nio.file.Files.createDirectory(DirBridge.defaultUpdatesDir);
}