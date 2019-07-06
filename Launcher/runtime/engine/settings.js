var settingsManagerClass = Java.extend(SettingsManagerClass.static, {
    getDefaultConfig: function() {
        var new_settings = new NewLauncherSettings;
        new_settings.login = null;
        new_settings.rsaPassword = null;
        new_settings.profile = 0;

        new_settings.updatesDir = DirBridge.defaultUpdatesDir;
        new_settings.autoEnter = config.autoEnterDefault;
        new_settings.fullScreen = config.fullScreenDefault;
        new_settings.ram = config.ramDefault;

        new_settings.featureStore = config.featureStoreDefault;
        new_settings.lastDigest = null;
        new_settings.lastProfiles.clear();
        new_settings.lastHDirs.clear();
        return new_settings;
    },
});

var settingsManager = new settingsManagerClass;
var settings = SettingsManager.settings;