var settingsClass = Java.extend(LauncherSettingsClass.static, {
    setDefault: function() {
        // Auth settings
        settings.login = null;
        settings.rsaPassword = null;
        settings.profile = 0;

        // Client settings
        settings.updatesDir = DirBridge.defaultUpdatesDir;
        settings.autoEnter = config.autoEnterDefault;
        settings.fullScreen = config.fullScreenDefault;
        settings.setRAM(config.ramDefault);

        // Offline cache
        settings.lastSign = null;
        settings.lastProfiles.clear();
        settings.lastHDirs.clear();

        // Apply CLI params
        cliParams.applySettings();
    },

    setPassword: function(password) {
        var encrypted = SecurityHelper.newRSAEncryptCipher(Launcher.getConfig().publicKey).doFinal(IOHelper.encode(password));
        settings.password = encrypted;
        return encrypted;
    }


});
var settingsOverlay = {
/* ===================== OVERLAY ===================== */
    overlay: null, ramLabel: null, dirLabel: null,
    deleteDirPressedAgain: false,

    initOverlay: function() {
        settings.overlay = loadFXML("dialog/overlay/settings/settings.fxml");

        // Lookup autoEnter checkbox
        var autoEnterBox = settings.overlay.lookup("#autoEnter");
        autoEnterBox.setSelected(settings.autoEnter);
        autoEnterBox.selectedProperty()["addListener(javafx.beans.value.ChangeListener)"](
            function(o, ov, nv) settings.autoEnter = nv);

        // Lookup fullScreen checkbox
        var fullScreenBox = settings.overlay.lookup("#fullScreen");
        fullScreenBox.setSelected(settings.fullScreen);
        fullScreenBox.selectedProperty()["addListener(javafx.beans.value.ChangeListener)"](
            function(o, ov, nv) settings.fullScreen = nv);

        // Lookup RAM label
        settings.ramLabel = settings.overlay.lookup("#ramLabel");
        settings.updateRAMLabel();

        // Lookup RAM slider options
        var ramSlider = settings.overlay.lookup("#ramSlider");
        ramSlider.setMin(0);
        ramSlider.setMax(JVMHelper.RAM);
        ramSlider.setSnapToTicks(true);
        ramSlider.setShowTickMarks(true);
        ramSlider.setShowTickLabels(true);
        ramSlider.setMinorTickCount(3);
        ramSlider.setMajorTickUnit(1024);
        ramSlider.setBlockIncrement(1024);
        ramSlider.setValue(settings.ram);
        ramSlider.valueProperty()["addListener(javafx.beans.value.ChangeListener)"](function(o, ov, nv) {
            settings.setRAM(nv);
            settings.updateRAMLabel();
        });

        // Lookup dir label
        settings.dirLabel = settings.overlay.lookup("#dirLabel");
        settings.dirLabel.setOnAction(function(event)
            app.getHostServices().showDocument(settings.updatesDir.toUri()));
        settings.updateDirLabel();

        // Lookup change dir button
        settings.overlay.lookup("#changeDir").setOnAction(function(event) {
            var chooser = new javafx.stage.DirectoryChooser();
            chooser.setTitle("Сменить директорию загрузок");
            chooser.setInitialDirectory(dir.toFile());

            // Set new result
            var newDir = chooser.showDialog(stage);
            if (newDir !== null) {
                settings.updatesDir = newDir.toPath();
                settings.updateDirLabel();
            }
        });

        // Lookup delete dir button
        var deleteDirButton = settings.overlay.lookup("#deleteDir");
        deleteDirButton.setOnAction(function(event) {
            if (!settings.deleteDirPressedAgain) {
                settings.deleteDirPressedAgain = true;
                deleteDirButton.setText("Подтвердить вменяемость");
                return;
            }

            // Delete dir!
            settings.deleteUpdatesDir();
            settings.deleteDirPressedAgain = false;
            deleteDirButton.setText("Ещё раз попробовать");
        });

        // Lookup debug checkbox
        var debugBox = settings.overlay.lookup("#debug");
        debugBox.setSelected(LogHelper.isDebugEnabled());
        debugBox.selectedProperty()["addListener(javafx.beans.value.ChangeListener)"](
            function(o, ov, nv) LogHelper.setDebugEnabled(nv));

        // Lookup apply settings button
        settings.overlay.lookup("#apply").setOnAction(function(event) overlay.hide(0, null));
    },

    updateRAMLabel: function() {
        settings.ramLabel.setText(settings.ram <= 0 ? "Автоматически" : settings.ram + " MiB");
    },

    deleteUpdatesDir: function() {
        processing.description.setText("Удаление директории загрузок");
        overlay.swap(0, processing.overlay, function(event) {
            var task = newTask(function() IOHelper.deleteDir(settings.updatesDir, false));
            task.setOnSucceeded(function(event) overlay.swap(0, settings.overlay, null));
            task.setOnFailed(function(event) {
                processing.setError(task.getException());
                overlay.swap(2500, settings.overlay, null);
            });
            startTask(task);
        });
    },

    updateDirLabel: function() {
        settings.dirLabel.setText(IOHelper.toString(settings.updatesDir));
    }
};
/* ====================== CLI PARAMS ===================== */
var cliParamsClass = Java.extend(CliParamsInterface.static, {
    login: null, password: null, profile: -1, autoLogin: false, // Auth
    updatesDir: null, autoEnter: null, fullScreen: null, ram: -1, // Client
    offline: false, // Offline

    init: function(params) {
        var named = params.getNamed();
        var unnamed = params.getUnnamed();

        // Read auth cli params
        cliParams.login = named.get("login");
        cliParams.password = named.get("password");
        var profile = named.get("profile");
        if (profile !== null) {
            cliParams.profile = java.lang.Integer.parseInt(profile);
        }
        cliParams.autoLogin = unnamed.contains("--autoLogin");

        // Read client cli params
        var updatesDir = named.get("updatesDir");
        if (updatesDir !== null) {
            cliParams.updatesDir = IOHelper.toPath(named.get("updatesDir"));
        }
        var autoEnter = named.get("autoEnter");
        if (autoEnter !== null) {
            cliParams.autoEnter = java.lang.Boolean.parseBoolean(autoEnter);
        }
        var fullScreen = named.get("fullScreen");
        if (fullScreen !== null) {
            cliParams.fullScreen = java.lang.Boolean.parseBoolean(fullScreen);
        }
        var ram = named.get("ram");
        if (ram !== null) {
            cliParams.ram = java.lang.Integer.parseInt(ram);
        }

        // Read offline cli param
        var offline = named.get("offline");
        if (offline !== null) {
            cliParams.offline = java.lang.Boolean.parseBoolean(offline);
        }
    },

    applySettings: function() {
        // Apply auth params
        if (cliParams.login !== null) {
            settings.login = cliParams.login;
        }
        if (cliParams.password !== null) {
            settings.setPassword(cliParams.password);
        }
        if (cliParams.profile >= 0) {
            settings.profile = cliParams.profile;
        }

        // Apply client params
        if (cliParams.updatesDir !== null) {
            settings.updatesDir = cliParams.updatesDir;
        }
        if (cliParams.autoEnter !== null) {
            settings.autoLogin = cliParams.autoEnter;
        }
        if (cliParams.fullScreen !== null) {
            settings.fullScreen = cliParams.fullScreen;
        }
        if (cliParams.ram >= 0) {
            settings.setRAM(cliParams.ram);
        }

        // Apply offline param
        if (cliParams.offline !== null) {
            settings.offline = cliParams.offline;
        }
    }
});
var cliParams = new cliParamsClass;
var settings = new settingsClass;
settings.cliParams = cliParams;