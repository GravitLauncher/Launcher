// ====== LAUNCHER CONFIG ====== //
var config = {
    dir: "GravitLauncher", // Launcher directory
    title: "GravitLauncher", // Window title
    icons: [ "favicon.png" ], // Window icon paths

    // Auth config
    linkText: "GravitLauncher", // Text for link under "Auth" button
    linkURL: new java.net.URL("https://gravitlauncher.ml"), // URL for link under "Auth" button
	
    // Menu config
    discord_url: new java.net.URL("https://discord.gg/bf7ZtwC"),

    // Settings defaults
    settingsMagic: 0xC0DE5, // Ancient magic, don't touch
    autoEnterDefault: false, // Should autoEnter be enabled by default?
    fullScreenDefault: false, // Should fullScreen be enabled by default?
    ramDefault: 1024, // Default RAM amount (0 for auto)
};

// ====== DON'T TOUCH! ====== //

DirBridge.dir = IOHelper.HOME_DIR.resolve(config.dir);
if (!IOHelper.isDir(DirBridge.dir)) {
    java.nio.file.Files.createDirectory(DirBridge.dir);
}
DirBridge.defaultUpdatesDir = DirBridge.dir.resolve("updates");
if (!IOHelper.isDir(DirBridge.defaultUpdatesDir)) {
    java.nio.file.Files.createDirectory(DirBridge.defaultUpdatesDir);
}

//====== SERVERS CONFIG ====== //
var serversConfig = {
    defaults: {
        // Лозунг сервера
        description: "Мир в котором возможно все"
    },
     getServerProperty: function(profile, property){
        if(serversConfig[profile]==null || serversConfig[profile][property]==null){
          return serversConfig.defaults[property];
        }
        return serversConfig[profile][property];
    }
};

var optModNames = {
   optAutoModName: true,//Попытатся автоматически создать представляемое имя модификации
   modInfo: {//"Путь до опц. модификации" : "Отображаемый клиенту контент"
     /*"mods/ModName-1.1.jar": {
        name: "ModName", //Наименование модификации (Отображаемое в лаунчере)
        description:"Лучший в своём роде ModName.", //Описание модификации
	group: 1, //Группа (Используется для ветки зависемых модификаций. К примеру: у NEI [submod: false и group: 1], а у NeiAddons и NeiPlugins [submod: true group: 1])
        submod: false //Это суб-модификация? (будет произведён отступ от левого края для выделения)
     },*/
   },
}
