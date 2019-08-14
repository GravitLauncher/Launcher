var settingsOverlay = {
    /* ===================== OVERLAY ===================== */
    overlay: null,
    ramLabel: null,
    dirLabel: null,
    transferDialog: null,
    deleteDirPressedAgain: false,
    count: 0,

    initOverlay: function() {
        settingsOverlay.overlay = loadFXML("dialog/overlay/settings/settings.fxml");

        var holder = settingsOverlay.overlay.lookup("#holder");

        var autoEnterBox = holder.lookup("#autoEnter");
        autoEnterBox.setSelected(settings.autoEnter);
        autoEnterBox.selectedProperty()["addListener(javafx.beans.value.ChangeListener)"](
            function(o, ov, nv) settings.autoEnter = nv);

        settingsOverlay.dirLabel = holder.lookup("#dirLabel");
        settingsOverlay.dirLabel.setOnAction(function(event) app.getHostServices().showDocument(settings.updatesDir.toUri()));
        settingsOverlay.updateDirLabel();

        settingsOverlay.transferDialog = holder.lookup("#transferDialog");
        settingsOverlay.transferDialog.setVisible(false);

        holder.lookup("#changeDir").setOnAction(function(event) {

            var chooser = new javafx.stage.DirectoryChooser();
            chooser.setTitle("Сменить директорию загрузок");
            chooser.setInitialDirectory(DirBridge.dir.toFile());

            var newDir = chooser.showDialog(stage);
            if (newDir !== null) {
                settingsOverlay.transferCatalogDialog(newDir.toPath());
            }
        });

        var featureStore = holder.lookup("#featureStore");
        featureStore.setSelected(settings.featureStore);
        featureStore.selectedProperty()["addListener(javafx.beans.value.ChangeListener)"](
            function(o, ov, nv) settings.featureStore = nv);

        var fullScreenBox = holder.lookup("#fullScreen");
        fullScreenBox.setSelected(settings.fullScreen);
        fullScreenBox.selectedProperty()["addListener(javafx.beans.value.ChangeListener)"](
            function(o, ov, nv) settings.fullScreen = nv);

        settingsOverlay.ramLabel = holder.lookup("#ramLabel");
        settingsOverlay.updateRAMLabel();

        var ramSlider = holder.lookup("#ramSlider");
        ramSlider.setMax(FunctionalBridge.getJVMTotalMemory());
        ramSlider.setSnapToTicks(true);
        ramSlider.setShowTickMarks(true);
        ramSlider.setShowTickLabels(true);
        ramSlider.setMinorTickCount(3);
        ramSlider.setMajorTickUnit(1024);
        ramSlider.setBlockIncrement(1024);
        ramSlider.setValue(settings.ram);
        ramSlider.valueProperty()["addListener(javafx.beans.value.ChangeListener)"](function(o, ov, nv) {
            settingsOverlay.setRAM(nv);
            settingsOverlay.updateRAMLabel();
        });

        var deleteDirButton = holder.lookup("#deleteDir");
        deleteDirButton.setOnAction(function(event) {
            if (!settingsOverlay.deleteDirPressedAgain) {
                settingsOverlay.deleteDirPressedAgain = true;
                deleteDirButton.setText("Подтвердить");
                return;
            }

            settingsOverlay.deleteUpdatesDir();
            settingsOverlay.deleteDirPressedAgain = false;
            settingsOverlay.count = settingsOverlay.count + 1;
            if (settingsOverlay.count > 9) {
                javafx.application.Platform.exit();
            }
            deleteDirButton.setText(
                settingsOverlay.count > 8 ? "Прощай :(" :
                (settingsOverlay.count > 7 ? "Я умираю!" :
                    (settingsOverlay.count > 5 ? "DeathCry, спаси!" :
                        (settingsOverlay.count > 4 ? "Умоляю, перестань!" :
                            (settingsOverlay.count > 3 ? "Да хорош уже!" : "Ещё раз")
                        ))));
        });

        var debugBox = settingsOverlay.overlay.lookup("#debug");
        debugBox.setSelected(settings.debug);
        debugBox.selectedProperty()["addListener(javafx.beans.value.ChangeListener)"](
            function(o, ov, nv) settings.debug = nv);

        holder.lookup("#apply").setOnAction(function(event) overlay.hide(0, null));
    },

    transferCatalogDialog: function(newDir) {
        settingsOverlay.transferDialog.setVisible(true);
        settingsOverlay.transferDialog.lookup("#cancelTransfer").setOnAction(function(event) {
            settings.updatesDir = newDir;
            DirBridge.dirUpdates = settings.updatesDir;
            settingsOverlay.updateDirLabel();
            settingsOverlay.transferDialog.setVisible(false);
        });
        settingsOverlay.transferDialog.lookup("#applyTransfer").setOnAction(function(event) {
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

    setPassword: function(password) {
        var encrypted = SecurityHelper.newRSAEncryptCipher(Launcher.getConfig().publicKey).doFinal(IOHelper.encode(password));
        return encrypted;
    },


    setRAM: function(ram) {
        if (ram > 762 && ram < 1024) {
            settings.ram = java.lang.Math["min(int,int)"](ram, FunctionalBridge.getJVMTotalMemory());
        } else {
            settings.ram = java.lang.Math["min(int,int)"](((ram / 256) | 0) * 256, FunctionalBridge.getJVMTotalMemory());
        }
    },

    updateDirLabel: function() {
        settingsOverlay.dirLabel.setText(IOHelper.toString(settings.updatesDir));
    }
};
LogHelper.debug("Dir: %s", DirBridge.dir);

/* ====================== CLI PARAMS ===================== */
var cliParams = {
    login: null,
    password: null,
    profile: -1,
    autoLogin: false,
    updatesDir: null,
    autoEnter: null,
    fullScreen: null,
    ram: -1,
    offline: false,
    featureStore: null,

    init: function(params) {
        var named = params.getNamed();
        var unnamed = params.getUnnamed();

        cliParams.login = named.get("login");
        cliParams.password = named.get("password");
        var profile = named.get("profile");
        if (profile !== null) {
            cliParams.profile = java.lang.Integer.parseInt(profile);
        }
        cliParams.autoLogin = unnamed.contains("--autoLogin");

        var updatesDir = named.get("updatesDir");
        if (updatesDir !== null) {
            cliParams.updatesDir = IOHelper.toPath(named.get("updatesDir"));
        }
        var autoEnter = named.get("autoEnter");
        if (autoEnter !== null) {
            cliParams.autoEnter = java.lang.Boolean.parseBoolean(autoEnter);
        }
        var featureStore = named.get("featureStore");
        if (featureStore !== null) {
            cliParams.featureStore = java.lang.Boolean.parseBoolean(featureStore);
        }
        var fullScreen = named.get("fullScreen");
        if (fullScreen !== null) {
            cliParams.fullScreen = java.lang.Boolean.parseBoolean(fullScreen);
        }
        var ram = named.get("ram");
        if (ram !== null) {
            cliParams.ram = java.lang.Integer.parseInt(ram);
        }
        var offline = named.get("offline");
        if (offline !== null) {
            cliParams.offline = java.lang.Boolean.parseBoolean(offline);
        }
    },

    applySettings: function() {
        if (cliParams.login !== null) {
            settings.login = cliParams.login;
        }
        if (cliParams.password !== null) {
            settingsOverlay.setPassword(cliParams.password);
        }
        if (cliParams.profile >= 0) {
            settings.profile = cliParams.profile;
        }
        if (cliParams.updatesDir !== null) {}
        if (cliParams.autoEnter !== null) {
            settings.autoLogin = cliParams.autoEnter;
        }
        if (cliParams.featureStore !== null) {
            settings.featureStore = cliParams.featureStore;
        }
        if (cliParams.fullScreen !== null) {
            settings.fullScreen = cliParams.fullScreen;
        }
        if (cliParams.ram >= 0) {
            settingsOverlay.setRAM(cliParams.ram);
        }
        if (cliParams.offline !== null) {
            settings.offline = cliParams.offline;
        }
    }
};