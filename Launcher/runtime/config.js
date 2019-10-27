var config = {
    //*** Настройки лаунчера ***//
    // Название папки лаунчера настраивается в LaunchServer.conf(строка projectName)
    title: "GravitLauncher",  // Заголовок окна
    icons: ["favicon.png"],   // Путь/Пути до иконки окна

    links: [
        //*** Ссылки ***//
        {
            id: "link",
            text: "GravitLauncher",
            url: "https://gravit.pro",
        },
    
        {
            id: "discord",
            text: "",
            url: "https://discord.gg/aJK6nMN",
        }
    ],

    //*** Стандартные настройки клиента ***//
    autoEnterDefault: false,    // Автоматический вход на выбранный сервер
    fullScreenDefault: false,   // Клиент в полный экран
    featureStoreDefault: true,  // Поиск файлов в других клиентах (Используется для экономии трафика и ускорения загрузки)
    ramDefault: 1024,           // Количество оперативной памяти выделенной по умолчанию (0 - Автоматически)

    //*** Настройка загрузки JVM ***//
    /* LaunchServer: guardtype = java */
    jvm: {
        enable: false,                       // Включение загрузки своей JVM
        jvmMustdie32Dir: "jre-8u231-win32",  // Название папки JVM для Windows x32
        jvmMustdie64Dir: "jre-8u231-win64",  // Название папки JVM для Windows x64
    },

    settingsMagic: 0xC0DE5, // Магия вне хогвартса
};

DirBridge.defaultUpdatesDir = DirBridge.dir.resolve("updates");
if (!IOHelper.isDir(DirBridge.defaultUpdatesDir)) {
    java.nio.file.Files.createDirectory(DirBridge.defaultUpdatesDir);
}