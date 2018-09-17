var settings = {
    file: dir.resolve("settings.bin"), // Settings file
    login: null, rsaPassword: null, profile: 0, // Auth
    updatesDir: null, autoEnter: false, fullScreen: false, ram: 0, // Client

    // Offline cache
    offline: false,
    lastSign: null,
    lastProfiles: new java.util.LinkedList(),
    lastHDirs: new java.util.HashMap(16),

    /* Settings and overlay functions */
    load: function() {
        LogHelper.debug("Loading settings file");
        try {
            tryWithResources(new HInput(IOHelper.newInput(settings.file)), settings.read);
        } catch(e) {
            LogHelper.error(e);
            settings.setDefault();
        }
    },

    save: function() {
        LogHelper.debug("Saving settings file");
        try {
            tryWithResources(new HOutput(IOHelper.newOutput(settings.file)), settings.write);
        } catch(e) {
            LogHelper.error(e);
        }
    },

    // Internal functions
    read: function(input) {
        var magic = input.readInt();
        if (magic != config.settingsMagic) {
            throw new java.io.IOException("Settings magic mismatch: " + java.lang.Integer.toString(magic, 16));
        }

        // Launcher settings
        var debug = input.readBoolean();
        if (!LogHelper.isDebugEnabled() && debug) {
            LogHelper.setDebugEnabled(true);
        }

        // Auth settings
        settings.login = input.readBoolean() ? input.readString(255) : null;
        settings.rsaPassword = input.readBoolean() ? input.readByteArray(IOHelper.BUFFER_SIZE) : null;
        settings.profile = input.readLength(0);

        // Client settings
        settings.updatesDir = IOHelper.toPath(input.readString(0));
        settings.autoEnter = input.readBoolean();
        settings.fullScreen = input.readBoolean();
        settings.setRAM(input.readLength(JVMHelper.RAM));

        // Offline cache
        var publicKey = Launcher.getConfig().publicKey;
        settings.lastSign = input.readBoolean() ? input.readByteArray(-SecurityHelper.RSA_KEY_LENGTH) : null;
        settings.lastProfiles.clear();
        var lastProfilesCount = input.readLength(0);
        for (var i = 0; i < lastProfilesCount; i++) {
            settings.lastProfiles.add(new SignedObjectHolder(input, publicKey, ClientProfile.RO_ADAPTER));
        }
        settings.lastHDirs.clear();
        var lastHDirsCount = input.readLength(0);
        for (var i = 0; i < lastHDirsCount; i++) {
            var name = IOHelper.verifyFileName(input.readString(255));
            VerifyHelper.putIfAbsent(settings.lastHDirs, name, new SignedObjectHolder(input, publicKey, function(i) new HashedDir(i)),
                java.lang.String.format("Duplicate offline hashed dir: '%s'", name));
        }

        // Apply CLI params
        cliParams.applySettings();
    },

    write: function(output) {
        output.writeInt(config.settingsMagic);

        // Launcher settings
        output.writeBoolean(LogHelper.isDebugEnabled());

        // Auth settings
        output.writeBoolean(settings.login !== null);
        if (settings.login !== null) {
            output.writeString(settings.login, 255);
        }
        output.writeBoolean(settings.rsaPassword !== null);
        if (settings.rsaPassword !== null) {
            output.writeByteArray(settings.rsaPassword, IOHelper.BUFFER_SIZE);
        }
        output.writeLength(settings.profile, 0);

        // Client settings
        output.writeString(IOHelper.toString(settings.updatesDir), 0);
        output.writeBoolean(settings.autoEnter);
        output.writeBoolean(settings.fullScreen);
        output.writeLength(settings.ram, JVMHelper.RAM);

        // Offline cache
        output.writeBoolean(settings.lastSign !== null);
        if (settings.lastSign !== null) {
            output.writeByteArray(settings.lastSign, -SecurityHelper.RSA_KEY_LENGTH);
        }
        output.writeLength(settings.lastProfiles.size(), 0);
        for each (var profile in settings.lastProfiles) {
            profile.write(output);
        }
        output.writeLength(settings.lastHDirs.size(), 0);
        for each (var entry in settings.lastHDirs.entrySet()) {
            output.writeString(entry.getKey(), 0);
            entry.getValue().write(output);
        }
    },

    setDefault: function() {
        // Auth settings
        settings.login = null;
        settings.rsaPassword = null;
        settings.profile = 0;

        // Client settings
        settings.updatesDir = defaultUpdatesDir;
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
    },

    setRAM: function(ram) {
        settings.ram = java.lang.Math["min(int,int)"](((ram / 256) | 0) * 256, JVMHelper.RAM);
    },

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
};
