// ====== LAUNCHER CONFIG ====== //
var config = {
    dir: "launcher", // Launcher directory
    title: "Minecraft Launcher", // Window title
    icons: [ "favicon.png" ], // Window icon paths

    // Auth config
    newsURL: "https://yii.gravithome.ru/index.php?r=blog%2Findex", // News WebView URL
    linkText: "GravitHome site", // Text for link under "Auth" button
    linkURL: new java.net.URL("https://gravithome.ru/"), // URL for link under "Auth" button

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
