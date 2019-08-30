var config = {
    //*** Настройки лаунчера ***//
    dir: "GravitLauncher",    // Название папки лаунчера
    title: "GravitLauncher",  // Заголовок окна
    icons: ["favicon.png"],   // Путь/Пути до иконки окна

    //*** Меню авторизации ***//
    linkText: "GravitLauncher",                                 // Текст ссылки
    linkURL: new java.net.URL("https://gravit.pro"),            // Ссылка

    //*** Меню выбора серверов ***//
    discord: new java.net.URL("https://discord.gg/aJK6nMN"),    // Ссылка

    //*** Стандартные настройки клиента ***//
    autoEnterDefault: false,    // Автоматический вход на выбранный сервер
    fullScreenDefault: false,   // Клиент в полный экран
    featureStoreDefault: true,  // Поиск файлов в других клиентах (Используется для экономии трафика и ускорения загрузки)
    ramDefault: 1024,           // Количество оперативной памяти выделенной по умолчанию (0 - Автоматически)

    //*** Настройка загрузки JVM ***//
    /* LaunchServer: guardtype = java */
    jvm: {
        enable: false,                       // Включение загрузки своей JVM
        jvmMustdie32Dir: "jre-8u211-win32",  // Название папки JVM для Windows x32
        jvmMustdie64Dir: "jre-8u211-win64",  // Название папки JVM для Windows x64
    },

    allowedOS: {
        windows: true,
        linux: true,
        macosx: true
    },

    settingsMagic: 0xC0DE5, // Магия вне хогвартса
};

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