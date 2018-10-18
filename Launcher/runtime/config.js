// ====== LAUNCHER CONFIG ====== //
var config = {
    dir: "gravitlauncher", // Launcher directory
    title: "Gravit Minecraft Launcher", // Window title
    icons: [ "favicon.png" ], // Window icon paths

    // Auth config
    newsURL: "https://github.com/GravitLauncher/Launcher/releases", // News WebView URL
    linkText: "GravitLauncher GitHub", // Text for link under "Auth" button
    linkURL: new java.net.URL("https://github.com/GravitLauncher/Launcher"), // URL for link under "Auth" button

    // Settings defaults
    settingsMagic: 0xC0DE5, // Ancient magic, don't touch
    autoEnterDefault: false, // Should autoEnter be enabled by default?
    fullScreenDefault: false, // Should fullScreen be enabled by default?
    ramDefault: 1024, // Default RAM amount (0 for auto)
};

// ====== DON'T TOUCH! ====== //
var dir = IOHelper.HOME_DIR.resolve(config.dir);
if (!IOHelper.isDir(dir)) {
    java.nio.file.Files.createDirectory(dir);
}
var defaultUpdatesDir = dir.resolve("updates");
if (!IOHelper.isDir(defaultUpdatesDir)) {
    java.nio.file.Files.createDirectory(defaultUpdatesDir);
}
