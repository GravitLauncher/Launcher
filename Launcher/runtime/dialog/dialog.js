// Ининциализируем кучу всяких переменных
var authPane, dimPane, serverPane;

// Переменные от окна входа
var loginField, passwordField, forgotButton, savePasswordBox, registerButton;

// Переменные от основной менюшки
var serverList, serverInfo, serverDescription, serverEntrance, serverLabel, serverStatus;
var discord_url;

// Прочие вспомогалки
var profilesList = []; // Ассоциативный массив: "кнопка сервера" => "профиль сервера"
var movePoint = null; // Координата, хранящая опроную точку при Drag'е
var pingers = {}; // ддосеры серверов
var loginData; // Буфер для данных авторизации

function initLauncher() {
	
    // Инициализируем основы
    initLoginScene();
    initMenuScene();

    // Инициализируем доп. менюшки
    debug.initOverlay();
    processing.initOverlay();
    settingsOverlay.initOverlay();
    update.initOverlay();
    options.initOverlay();

    // Делаем запрос на проверку свежести лаунчера, ну и сервера заодно обновляем
    verifyLauncher();
}

function initLoginScene() {
    loginPane.setOnMousePressed(function(event){ movePoint = new javafx.geometry.Point2D(event.getSceneX(), event.getSceneY())});
    loginPane.setOnMouseDragged(function(event) {
        if(movePoint === null) {
            return;
        }

        // Обновляем позицию панели
        stage.setX(event.getScreenX() - movePoint.getX());
        stage.setY(event.getScreenY() - movePoint.getY());
    });
    loginPane.lookup("#exitbtn").setOnAction(function(event){ javafx.application.Platform.exit()});
    loginPane.lookup("#hidebtn").setOnAction(function(event){ stage.setIconified(true)});
    loginPane.lookup("#discord_url").setOnAction(function(){ openURL(config.discord_url); });

    var pane = loginPane.lookup("#authPane");
    authPane = pane;

    // Lookup login field
    loginField = pane.lookup("#login");
	loginField.setOnMouseMoved(function(event){rootPane.fireEvent(event)}); 
    loginField.setOnAction(goAuth);
    if (settings.login !== null) {
        loginField.setText(settings.login);
    }

    // Lookup password field
    passwordField = pane.lookup("#password");
	passwordField.setOnMouseMoved(function(event){rootPane.fireEvent(event)});
    passwordField.setOnAction(goAuth);
    if (settings.rsaPassword !== null) {
        passwordField.getStyleClass().add("hasSaved");
        passwordField.setPromptText("*** Сохранённый ***");
    }
	
    // Lookup save password box
    savePasswordBox = pane.lookup("#rememberchb");
    savePasswordBox.setSelected(settings.login === null || settings.rsaPassword !== null);
	
    // Lookup hyperlink text and actions
    var link = pane.lookup("#link");
    link.setText(config.linkText);
    link.setOnAction(function(event) app.getHostServices().showDocument(config.linkURL.toURI()));
	
    // Lookup action buttons
    pane.lookup("#goAuth").setOnAction(goAuth);
}

function initMenuScene() {
    menuPane.setOnMousePressed(function(event){ movePoint = new javafx.geometry.Point2D(event.getSceneX(), event.getSceneY())});
    menuPane.setOnMouseDragged(function(event) {
        if(movePoint === null) {
            return;
        }

        // Обновляем позицию панели
        stage.setX(event.getScreenX() - movePoint.getX());
        stage.setY(event.getScreenY() - movePoint.getY());
    });
    menuPane.lookup("#exitbtn").setOnAction(function(event){ javafx.application.Platform.exit()});
    menuPane.lookup("#hidebtn").setOnAction(function(event){ stage.setIconified(true)});
    var pane = menuPane.lookup("#serverPane");
    serverPane = pane;

    menuPane.lookup("#discord_url").setOnAction(function(){ openURL(config.discord_url); });

    pane.lookup("#settingsbtn").setOnAction(goSettings);
    pane.lookup("#clientbtn").setOnAction(goOptions);
    serverList = pane.lookup("#serverlist").getContent();
    serverInfo = pane.lookup("#serverinfo").getContent();
    serverDescription = serverInfo.lookup("#serverDescription");

    serverEntrance = pane.lookup("#serverentrance");
    serverStatus = serverEntrance.lookup("#serverStatus");
    serverLabel = serverEntrance.lookup("#serverLabel");
    serverEntrance.lookup("#serverLaunch").setOnAction(function(){
        doUpdate(profilesList[serverHolder.old], loginData.pp, loginData.accessToken);
    });

    pane.lookup("#logoutbtn").setOnAction(function(){
        setCurrentScene(loginScene);
    });
}

function initOffline() {
    // Меняем заголовок(Хер его знает зачем, его всё равно нигде не видно...
    stage.setTitle(config.title + " [Offline]");

    // Set login field as username field
    loginField.setPromptText("Имя пользователя");
    if (!VerifyHelper.isValidUsername(settings.login)) {
        loginField.setText(""); // Reset if not valid
    }


    // Disable password field
    passwordField.setDisable(true);
    passwordField.setPromptText("Недоступно");
    passwordField.setText("");
}

/* ======== Handler functions ======== */
function goAuth(event) {
    // Verify there's no other overlays
    if (overlay.current !== null) {
        return;
    } 

    // Get login
    var login = loginField.getText();
    if (login.isEmpty()) {
        return; // Maybe throw exception?)
    }

    // Get password if online-mode
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

    // Show auth overlay
    settings.login = login;
    doAuth(login, rsaPassword);
}

function goSettings(event) {
    // Verify there's no other overlays
    if (overlay.current !== null) {
        return;
    }

    // Show settings overlay
    overlay.show(settingsOverlay.overlay, null);
}

function goOptions(event) {
    // Verify there's no other overlays
    if (overlay.current !== null) {
        return;
    }

    // Show options overlay
    options.update();
    overlay.show(options.overlay, null);
}

/* ======== Processing functions ======== */
function verifyLauncher(e) {
    processing.resetOverlay();
    overlay.show(processing.overlay, function(event) makeLauncherRequest(function(result) {
        settings.lastDigest = result.digest;
        processing.resetOverlay();
        // Init offline if set
        if (settings.offline) {
             initOffline();
        }
        overlay.swap(0, processing.overlay, function(event) makeProfilesRequest(function(result) {
            settings.lastProfiles = result.profiles;
            // Update profiles list and hide overlay
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

            // Update asset dir
            update.resetOverlay("Обновление файлов ресурсов");
            var assetDirName = profile.getAssetDir();
            var assetDir = settings.updatesDir.resolve(assetDirName);
            var assetMatcher = profile.getAssetUpdateMatcher();
            makeSetProfileRequest(profile, function() {
                ClientLauncher.setProfile(profile);
                makeUpdateRequest(assetDirName, assetDir, assetMatcher, digest, function(assetHDir) {
                    settings.lastHDirs.put(assetDirName, assetHDir.hdir);

                    // Update client dir
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

    // Switch to debug overlay
    debug.resetOverlay();
    overlay.swap(0, debug.overlay, function(event) debugProcess(process));
}

/* ======== Server handler functions ======== */
function updateProfilesList(profiles) {
    profilesList = [];
    // Set profiles items
    serverList.getChildren().clear();
    var index = 0;
    profiles.forEach(function (profile, i, arr) {
        pingers[profile] = new ServerPinger(profile.getServerSocketAddress(), profile.getVersion());
        var serverBtn = new javafx.scene.control.ToggleButton(profile);
        (function () {
            profilesList[serverBtn] = profile;
            var hold = serverBtn;
            var hIndex = index;
            serverBtn.setOnAction(function (event) {
                serverHolder.set(hold);
                settings.profile = hIndex;
            });
        })();
        serverList.getChildren().add(serverBtn);
        if(profile.getOptional() != null) profile.updateOptionalGraph();
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

    // Launch transition
    transition.setDelay(javafx.util.Duration.millis(delay));
    transition.setFromValue(from);
    transition.setToValue(to);
    transition.play();
}

var overlay = {
    current: null,

    show: function(newOverlay, onFinished) {
        // Freeze root pane
        authPane.setDisable(true);
        overlay.current = newOverlay;

        // Show dim pane
        dimPane.setVisible(true);
        dimPane.toFront();

        // Fade dim pane
        fade(dimPane, 0.0, 0.0, 1.0, function(event) {
            dimPane.requestFocus();
            dimPane.getChildren().add(newOverlay);

            // Fix overlay position
            newOverlay.setLayoutX((dimPane.getPrefWidth() - newOverlay.getPrefWidth()) / 2.0);
            newOverlay.setLayoutY((dimPane.getPrefHeight() - newOverlay.getPrefHeight()) / 2.0);

            // Fade in
            fade(newOverlay, 0.0, 0.0, 1.0, onFinished);
        });
    },

    hide: function(delay, onFinished) {
        fade(overlay.current, delay, 1.0, 0.0, function(event) {
            dimPane.getChildren().remove(overlay.current);
            fade(dimPane, 0.0, 1.0, 0.0, function(event) {
                dimPane.setVisible(false);

                // Unfreeze root pane
                authPane.setDisable(false);
                rootPane.requestFocus();

                // Reset overlay state
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
            // Hide old overlay
            if (overlay.current !== newOverlay) {
                var child = dimPane.getChildren();
                child.set(child.indexOf(overlay.current), newOverlay);
            }

            // Fix overlay position
            newOverlay.setLayoutX((dimPane.getPrefWidth() - newOverlay.getPrefWidth()) / 2.0);
            newOverlay.setLayoutY((dimPane.getPrefHeight() - newOverlay.getPrefHeight()) / 2.0);

            // Show new overlay
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

/* ======== Overlay scripts ======== */
launcher.loadScript("dialog/overlay/debug/debug.js");
launcher.loadScript("dialog/overlay/processing/processing.js");
launcher.loadScript("dialog/overlay/settings/settings.js");
launcher.loadScript("dialog/overlay/options/options.js");
launcher.loadScript("dialog/overlay/update/update.js");
