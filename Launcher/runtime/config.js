var config = {
    //*** Настройки лаунчера ***/
    dir: "GravitLauncher",    // Название папки лаунчера
    title: "GravitLauncher",  // Заголовок окна

    //*** Меню авторизации ***/
    linkText: "GravitLauncher",                              // Текст ссылки
    linkURL: new java.net.URL("https://gravitlauncher.ml"),  // Ссылка на страницу в браузере

    //*** Меню выбора серверов ***/
    discord: new java.net.URL("https://discord.gg/aJK6nMN"),

    //*** Стандартные настройки клиента ***//
    autoEnterDefault: false,    // Автоматический вход на выбранный сервер
    fullScreenDefault: false,   // Клиент в полный экран
    featureStoreDefault: true,  // Поистк файлов в других клиентах (Используется для экономии трафика и ускорения загрузки)
    ramDefault: 1024,           // Количество оперативной памяти выделенной по умолчанию (0 - Автоматически)

    //*** Настройка загрузки JVM ***//
    /* LaunchServer: guardtype = java */
    jvm: {
        enable: false,                       // Включение загрузки своей JVM
        jvmMustdie32Dir: "jre-8u211-win32",  // Название папки JVM для Windows x32
        jvmMustdie64Dir: "jre-8u211-win64",  // Название папки JVM для Windows x64
    },

    settingsMagic: 0xC0DE5, // Магия вне хогвартса
};