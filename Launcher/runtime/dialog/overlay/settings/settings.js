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
        settings.lastDigest = null;
        settings.lastProfiles.clear();
        settings.lastHDirs.clear();

        // Apply CLI params
        cliParams.applySettings();
    },

    setPassword: function(password) {
        var encrypted = SecurityHelper.newRSAEncryptCipher(Launcher.getConfig().publicKey).doFinal(IOHelper.encode(password));
        //settings.password = encrypted;
        return encrypted;
    },


    setRAM: function(ram) {
		if (ram>762&&ram<1024){
        settings.ram = java.lang.Math["min(int,int)"](ram, FunctionalBridge.getJVMTotalMemory());
		}else{
        settings.ram = java.lang.Math["min(int,int)"](((ram / 256) | 0) * 256, FunctionalBridge.getJVMTotalMemory());
		}
    },
});
var settingsOverlay = {
/* ===================== OVERLAY ===================== */
    overlay: null, ramLabel: null, dirLabel: null, transferDialog: null,
    deleteDirPressedAgain: false, count: 0,

    initOverlay: function() {
        settingsOverlay.overlay = loadFXML("dialog/overlay/settings/settings.fxml");

        // Lookup autoEnter checkbox
        var holder = settingsOverlay.overlay.lookup("#holder");

        var autoEnterBox = holder.lookup("#autoEnter");
        autoEnterBox.setSelected(settings.autoEnter);
        autoEnterBox.selectedProperty()["addListener(javafx.beans.value.ChangeListener)"](
            function(o, ov, nv) settings.autoEnter = nv);

        // Lookup dir label
        settingsOverlay.dirLabel = holder.lookup("#dirLabel");
        settingsOverlay.dirLabel.setOnAction(function(event)
            app.getHostServices().showDocument(settings.updatesDir.toUri()));
        settingsOverlay.updateDirLabel();

        // Lokup transferDialog pane
        settingsOverlay.transferDialog = holder.lookup("#transferDialog");
        settingsOverlay.transferDialog.setVisible(false);
		
		// Lookup change dir button
        holder.lookup("#changeDir").setOnAction(function(event) {
            var chooser = new javafx.stage.DirectoryChooser();
            chooser.setTitle("Сменить директорию загрузок");
            chooser.setInitialDirectory(DirBridge.dir.toFile());

            // Set new result
            var newDir = chooser.showDialog(stage);
            if (newDir !== null) {
                settingsOverlay.transferCatalogDialog(newDir.toPath());
            }
        });

        // Lookup fullScreen checkbox
        var fullScreenBox = holder.lookup("#fullScreen");
        fullScreenBox.setSelected(settings.fullScreen);
        fullScreenBox.selectedProperty()["addListener(javafx.beans.value.ChangeListener)"](
            function(o, ov, nv) settings.fullScreen = nv);

        // Lookup RAM label
        settingsOverlay.ramLabel = holder.lookup("#ramLabel");
        settingsOverlay.updateRAMLabel();

        // Lookup RAM slider options
        var ramSlider = holder.lookup("#ramSlider");
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
            settingsOverlay.updateRAMLabel();
        });

        // Lookup delete dir button
        var deleteDirButton = holder.lookup("#deleteDir");
        deleteDirButton.setOnAction(function(event) {
            if (!settingsOverlay.deleteDirPressedAgain) {
                settingsOverlay.deleteDirPressedAgain = true;
                deleteDirButton.setText("Подтвердить");
                return;
            }

            // Delete dir!
            settingsOverlay.deleteUpdatesDir();
            settingsOverlay.deleteDirPressedAgain = false;
            settingsOverlay.count = settingsOverlay.count+1;
			if(settingsOverlay.count>9){
				javafx.application.Platform.exit();
			}
            deleteDirButton.setText(
				settingsOverlay.count>8?"Прощай :(":
				(settingsOverlay.count>7?"Я умираю!":
				(settingsOverlay.count>5?"DeathCry, спаси!":
				(settingsOverlay.count>4?"Умоляю, перестань!":
				(settingsOverlay.count>3?"Да хорош уже!":"Ещё раз")
			))));
        });

        // Lookup debug checkbox
        var debugBox = settingsOverlay.overlay.lookup("#debug");
        debugBox.setSelected(LogHelper.isDebugEnabled());
        debugBox.selectedProperty()["addListener(javafx.beans.value.ChangeListener)"](
            function(o, ov, nv) LogHelper.setDebugEnabled(nv));


        // Lookup apply settings button
        holder.lookup("#apply").setOnAction(function(event) overlay.hide(0, null));
    },

    transferCatalogDialog: function(newDir) {
        settingsOverlay.transferDialog.setVisible(true);
        settingsOverlay.transferDialog.lookup("#cancelTransfer").setOnAction(function(event)
        {
            settings.updatesDir = newDir;
            DirBridge.dirUpdates = settings.updatesDir;
            settingsOverlay.updateDirLabel();
            settingsOverlay.transferDialog.setVisible(false);
        });
        settingsOverlay.transferDialog.lookup("#applyTransfer").setOnAction(function(event) {
            //Здесь могла быть ваша реклама, либо DirBridge.move();
            DirBridge.move(newDir);
            settings.updatesDir = newDir;
            DirBridge.dirUpdates = settings.updatesDir;
            settingsOverlay.updateDirLabel();
            settingsOverlay.transferDialog.setVisible(false);
        });
    },

    updateRAMLabel: function() {
        settingsOverlay.ramLabel.setText(settings.ram <= 0 ? "Автоматически" : settings.ram + " MiB");
    },

    deleteUpdatesDir: function() {
        processing.description.setText("Удаление директории загрузок");
        overlay.swap(0, processing.overlay, function(event) {
            var task = newTask(function() IOHelper.deleteDir(settings.updatesDir, false));
            task.setOnSucceeded(function(event) overlay.swap(0, settingsOverlay.overlay, null));
            task.setOnFailed(function(event) {
                processing.setError(task.getException());
                overlay.swap(2500, settingsOverlay.overlay, null);
            });
            startTask(task);
        });
    },

    updateDirLabel: function() {
        settingsOverlay.dirLabel.setText(IOHelper.toString(settings.updatesDir));
    }
};
LogHelper.debug("Dir: %s", DirBridge.dir);
var settings = new settingsClass;
/* ====================== CLI PARAMS ===================== */
var cliParams = {
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
            //settings.updatesDir = cliParams.updatesDir;
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
};
