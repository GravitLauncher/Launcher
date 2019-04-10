var authPane, dimPane, serverPane, bar, optionsPane, consolePane;
var loginField, passwordField, savePasswordBox;
var serverList, serverInfo, serverDescription, serverEntrance, serverLabel, serverStatus;
var profilesList = [];
var movePoint = null;
var pingers = {};
var loginData;

function initLauncher() {
    initLoginScene();
    initMenuScene();
    initConsoleScene();
    initOptionsScene();

    /* ======== init Overlays ======== */
    debug.initOverlay();
    processing.initOverlay();
    settingsOverlay.initOverlay();
    update.initOverlay();

    verifyLauncher();
}

/* ======== init Login window======== */
function initLoginScene() {
    loginPane.setOnMousePressed(function(event){ movePoint = new javafx.geometry.Point2D(event.getSceneX(), event.getSceneY())});
    loginPane.setOnMouseDragged(function(event) {
        if(movePoint === null) {
            return;
        }
        stage.setX(event.getScreenX() - movePoint.getX());
        stage.setY(event.getScreenY() - movePoint.getY());
    });

    var pane = loginPane.lookup("#bar");
    bar = pane;
    loginPane.lookup("#close").setOnAction(function(event){ javafx.application.Platform.exit()});
    loginPane.lookup("#hide").setOnAction(function(event){ stage.setIconified(true)});
    loginPane.lookup("#discord").setOnAction(function(){ openURL(config.discord); });

    var pane = loginPane.lookup("#authPane");
    authPane = pane;

    loginField = pane.lookup("#login");
    loginField.setOnMouseMoved(function(event){rootPane.fireEvent(event)});
    loginField.setOnAction(goAuth);
    if (settings.login !== null) {
        loginField.setText(settings.login);
    }

    passwordField = pane.lookup("#password");
    passwordField.setOnMouseMoved(function(event){rootPane.fireEvent(event)});
    passwordField.setOnAction(goAuth);
    if (settings.rsaPassword !== null) {
        passwordField.getStyleClass().add("hasSaved");
        passwordField.setPromptText("*** Сохранённый ***");
    }

    savePasswordBox = pane.lookup("#rememberchb");
    savePasswordBox.setSelected(settings.login === null || settings.rsaPassword !== null);

    var link = pane.lookup("#link");
    link.setText(config.linkText);
    link.setOnAction(function(event) app.getHostServices().showDocument(config.linkURL.toURI()));

    pane.lookup("#goAuth").setOnAction(goAuth);
}

/* ======== init Menu window======== */
function initMenuScene() {
    menuPane.setOnMousePressed(function(event){ movePoint = new javafx.geometry.Point2D(event.getSceneX(), event.getSceneY())});
    menuPane.setOnMouseDragged(function(event) {
        if(movePoint === null) {
            return;
        }

        stage.setX(event.getScreenX() - movePoint.getX());
        stage.setY(event.getScreenY() - movePoint.getY());
    });

    var pane = loginPane.lookup("#bar");
    bar = pane;
    menuPane.lookup("#close").setOnAction(function(event){ javafx.application.Platform.exit()});
    menuPane.lookup("#hide").setOnAction(function(event){ stage.setIconified(true)});
    menuPane.lookup("#discord").setOnAction(function(){ openURL(config.discord); });
    menuPane.lookup("#settings").setOnAction(goSettings);
    menuPane.lookup("#goConsole").setOnAction(goConsole);
    menuPane.lookup("#logout").setOnAction(function(){
        setCurrentScene(loginScene);
    });

    var pane = menuPane.lookup("#serverPane");
    serverPane = pane;

    pane.lookup("#clientSettings").setOnAction(goOptions);
    serverList = pane.lookup("#serverlist").getContent();
    serverInfo = pane.lookup("#serverinfo").getContent();
    serverDescription = serverInfo.lookup("#serverDescription");
    serverEntrance = pane.lookup("#serverentrance");
    serverStatus = serverEntrance.lookup("#serverStatus");
    serverLabel = serverEntrance.lookup("#serverLabel");
    serverEntrance.lookup("#clientLaunch").setOnAction(function(){
        doUpdate(profilesList[serverHolder.old], loginData.pp, loginData.accessToken);
    });

}

/* ======== init Console window======== */
function initConsoleScene() {
    consoleMenu.setOnMousePressed(function(event){ movePoint = new javafx.geometry.Point2D(event.getSceneX(), event.getSceneY())});
    consoleMenu.setOnMouseDragged(function(event) {
        if(movePoint === null) {
            return;
        }

        stage.setX(event.getScreenX() - movePoint.getX());
        stage.setY(event.getScreenY() - movePoint.getY());
    });

    var pane = consoleMenu.lookup("#bar");
    bar = pane;
    pane.lookup("#close").setOnAction(function(event){ javafx.application.Platform.exit()});
    pane.lookup("#hide").setOnAction(function(event){ stage.setIconified(true)});
    pane.lookup("#back").setOnAction(function(){
        setCurrentScene(menuScene);
    });

    var pane = consoleMenu.lookup("#consolePane");
    consolePane = pane;

}

/* ======== init Options window======== */
function initOptionsScene() {
    optionsMenu.setOnMousePressed(function(event){ movePoint = new javafx.geometry.Point2D(event.getSceneX(), event.getSceneY())});
    optionsMenu.setOnMouseDragged(function(event) {
        if(movePoint === null) {
            return;
        }

        stage.setX(event.getScreenX() - movePoint.getX());
        stage.setY(event.getScreenY() - movePoint.getY());
    });

    var pane = optionsMenu.lookup("#bar");
    bar = pane;
    pane.lookup("#close").setOnAction(function(event){ javafx.application.Platform.exit()});
    pane.lookup("#hide").setOnAction(function(event){ stage.setIconified(true)});
    pane.lookup("#back").setOnAction(function(){
        setCurrentScene(menuScene);
    });
}

/* ======== init Offline ======== */
function initOffline() {
    stage.setTitle(config.title + " [Offline]");

    loginField.setPromptText("Имя пользователя");
    if (!VerifyHelper.isValidUsername(settings.login)) {
        loginField.setText(""); // Reset if not valid
    }

    passwordField.setDisable(true);
    passwordField.setPromptText("Недоступно");
    passwordField.setText("");
}

/* ======== Auth ======== */
function goAuth(event) {
    if (overlay.current !== null) {
        return;
    }

    var login = loginField.getText();
    if (login.isEmpty()) {
        return;
    }

    var rsaPassword = null;
    if (!passwordField.isDisable()) {
        var password = passwordField.getText();
        if (password !== null && !password.isEmpty()) {
            rsaPassword = settings.setPassword(password);
        } else if (settings.rsaPassword !== null) {
            rsaPassword = settings.rsaPassword;
        } else {
            return;
        }

        settings.rsaPassword = savePasswordBox.isSelected() ? rsaPassword : null;
    }

    settings.login = login;
    doAuth(login, rsaPassword);
}

/* ======== Console ======== */
function goConsole(event) {
    setCurrentScene(consoleScene);
}

/* ======== Settings ======== */
function goSettings(event) {
    if (overlay.current !== null) {
        return;
    }

    overlay.show(settingsOverlay.overlay, null);
}

/* ======== Options ======== */
function goOptions(event) {
    setCurrentScene(optionsScene);

    options.update();
}

/* ======== Processing functions ======== */
function verifyLauncher(e) {
    processing.resetOverlay();
    overlay.show(processing.overlay, function(event) makeLauncherRequest(function(result) {
        settings.lastDigest = result.digest;
        processing.resetOverlay();
        if (settings.offline) {
            initOffline();
        }
        overlay.swap(0, processing.overlay, function(event) makeProfilesRequest(function(result) {
            settings.lastProfiles = result.profiles;
            updateProfilesList(result.profiles);
            options.load();
            overlay.hide(0, function() {
                if (cliParams.autoLogin) {
                    goAuth(null);
                }
            });
        }));
    }));
}

function doAuth(login, rsaPassword) {
    processing.resetOverlay();
    overlay.show(processing.overlay, function (event) {
        FunctionalBridge.getHWID.join();
        makeAuthRequest(login, rsaPassword, function (result) {
            FunctionalBridge.setAuthParams(result);
            loginData = { pp: result.playerProfile , accessToken: result.accessToken, permissions: result.permissions};

            overlay.hide(0, function () {
                setCurrentScene(menuScene);
            });
            return result;
        })
    });
}

function doUpdate(profile, pp, accessToken) {
    var digest = profile.isUpdateFastCheck();
    overlay.swap(0, update.overlay, function(event) {

        update.resetOverlay("Обновление файлов ресурсов");
        var assetDirName = profile.getAssetDir();
        var assetDir = settings.updatesDir.resolve(assetDirName);
        var assetMatcher = profile.getAssetUpdateMatcher();
        makeSetProfileRequest(profile, function() {
            ClientLauncher.setProfile(profile);
            makeUpdateRequest(assetDirName, assetDir, assetMatcher, digest, function(assetHDir) {
                settings.lastHDirs.put(assetDirName, assetHDir.hdir);

                update.resetOverlay("Обновление файлов клиента");
                var clientDirName = profile.getDir();
                var clientDir = settings.updatesDir.resolve(clientDirName);
                var clientMatcher = profile.getClientUpdateMatcher();
                makeUpdateRequest(clientDirName, clientDir, clientMatcher, digest, function(clientHDir) {
                    settings.lastHDirs.put(clientDirName, clientHDir.hdir);
                    doLaunchClient(assetDir, assetHDir.hdir, clientDir, clientHDir.hdir, profile, pp, accessToken);
                });
            });
        });
    });
}

function doLaunchClient(assetDir, assetHDir, clientDir, clientHDir, profile, pp, accessToken) {
    processing.resetOverlay();
    overlay.swap(0, processing.overlay, function(event)
    launchClient(assetHDir, clientHDir, profile, new ClientLauncherParams(settings.lastDigest,
        assetDir, clientDir, pp, accessToken, settings.autoEnter, settings.fullScreen, settings.ram, 0, 0), doDebugClient)
);
}

function doDebugClient(process) {
    if (!LogHelper.isDebugEnabled()) {
        javafx.application.Platform.exit();
        return;
    }

    debug.resetOverlay();
    overlay.swap(0, debug.overlay, function(event) debugProcess(process));
}

/* ======== Server handler functions ======== */
function updateProfilesList(profiles) {
    profilesList = [];
    serverList.getChildren().clear();
    var index = 0;
    profiles.forEach(function(profile, i, arr) {
        pingers[profile] = new ServerPinger(profile.getServerSocketAddress(), profile.getVersion());

        var serverBtn = new javafx.scene.control.ToggleButton(profile);

        serverBtn.getStyleClass().add("server-button");
        serverBtn.getStyleClass().add("server-button-" + profile);

        (function() {
            profilesList[serverBtn] = profile;
            var hold = serverBtn;
            var hIndex = index;
            serverBtn.setOnAction(function(event) {
                serverHolder.set(hold);
                settings.profile = hIndex;
            });
        })();

        serverList.getChildren().add(serverBtn);
        if (profile.getOptional() != null) profile.updateOptionalGraph();
        index++;
    });
    LogHelper.debug("Load selected %d profile",settings.profile);
    if(profiles.length > 0) {
        if(settings.profile >= profiles.length)
            settings.profile = profiles.length-1;
        serverHolder.set(serverList.getChildren().get(settings.profile));
    }
}

function pingServer(btn) {
    var profile = profilesList[btn];
    setServerStatus("...");
    var task = newTask(function() pingers[profile].ping());
    task.setOnSucceeded(function(event) {
        var result = task.getValue();
        if(btn==serverHolder.old){
            setServerStatus(java.lang.String.format("%d из %d", result.onlinePlayers, result.maxPlayers));
        }
    });
    task.setOnFailed(function(event){ if(btn==serverHolder.old){setServerStatus("Недоступен")}});
    startTask(task);
}

function setServerStatus(description) {
    serverStatus.setText(description);
}

/* ======== Overlay helper functions ======== */
function fade(region, delay, from, to, onFinished) {
    var transition = new javafx.animation.FadeTransition(javafx.util.Duration.millis(100), region);
    if (onFinished !== null) {
        transition.setOnFinished(onFinished);
    }

    transition.setDelay(javafx.util.Duration.millis(delay));
    transition.setFromValue(from);
    transition.setToValue(to);
    transition.play();
}

var overlay = {
    current: null,

    show: function(newOverlay, onFinished) {
        authPane.setDisable(true);
        overlay.current = newOverlay;

        dimPane.setVisible(true);
        dimPane.toFront();

        fade(dimPane, 0.0, 0.0, 1.0, function(event) {
            dimPane.requestFocus();
            dimPane.getChildren().add(newOverlay);

            newOverlay.setLayoutX((dimPane.getPrefWidth() - newOverlay.getPrefWidth()) / 2.0);
            newOverlay.setLayoutY((dimPane.getPrefHeight() - newOverlay.getPrefHeight()) / 2.0);

            fade(newOverlay, 0.0, 0.0, 1.0, onFinished);
        });
    },

    hide: function(delay, onFinished) {
        fade(overlay.current, delay, 1.0, 0.0, function(event) {
            dimPane.getChildren().remove(overlay.current);
            fade(dimPane, 0.0, 1.0, 0.0, function(event) {
                dimPane.setVisible(false);

                authPane.setDisable(false);
                rootPane.requestFocus();

                overlay.current = null;
                if (onFinished !== null) {
                    onFinished();
                }
            });
        });
    },

    swap: function(delay, newOverlay, onFinished) {
        dimPane.toFront();
        fade(overlay.current, delay, 1.0, 0.0, function(event) {
            dimPane.requestFocus();

            if(overlay.current==null){
                overlay.show(newOverlay, onFinished);
                return;
            }

            if (overlay.current !== newOverlay) {
                var child = dimPane.getChildren();
                child.set(child.indexOf(overlay.current), newOverlay);
            }

            newOverlay.setLayoutX((dimPane.getPrefWidth() - newOverlay.getPrefWidth()) / 2.0);
            newOverlay.setLayoutY((dimPane.getPrefHeight() - newOverlay.getPrefHeight()) / 2.0);

            overlay.current = newOverlay;
            fade(newOverlay, 0.0, 0.0, 1.0, onFinished);
        });
    }
};

var serverHolder = {
    old: null,

    set: function(btn){
        pingServer(btn);
        serverLabel.setText("СЕРВЕР " + profilesList[btn]);
        serverDescription.setText(profilesList[btn].info);
        btn.setSelected(true);
        btn.setDisable(true);
        if(serverHolder.old!=null){
            serverHolder.old.setSelected(false);
            serverHolder.old.setDisable(false);
        }
        serverHolder.old = btn;
    }
};

/* ======== Scenes scripts ======== */
launcher.loadScript("dialog/overlay/debug/debug.js");
launcher.loadScript("dialog/overlay/processing/processing.js");
launcher.loadScript("dialog/overlay/settings/settings.js");
launcher.loadScript("dialog/overlay/update/update.js");

/* ======== Overlays scripts ======== */
launcher.loadScript("dialog/scenes/options/options.js");
