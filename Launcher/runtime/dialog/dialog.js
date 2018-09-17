// Dialog nodes
var rootPane, news, authPane, dimPane;
var loginField, passwordField, savePasswordBox, profilesBox;
var movePoint = null; // Point2D

// State variables
var pingers = {};

function initDialog() {
    // Lookup news WebView
    news = rootPane.lookup("#news");
    var newsEngine = news.getEngine();
    newsEngine.setUserDataDirectory(dir.resolve("webview").toFile());
    newsEngine.load(config.newsURL);

    // Lookup auth pane and dim
    initAuthPane(rootPane.lookup("#authPane"));
    dimPane = rootPane.lookup("#dim");

    // Init overlays
    debug.initOverlay();
    processing.initOverlay();
    settings.initOverlay();
    update.initOverlay();

    // Verify launcher & make request
    verifyLauncher();
}

function initAuthPane(pane) {
    authPane = pane;

    // Lookup login field
    loginField = pane.lookup("#login");
    loginField.setOnAction(goAuth);
    if (settings.login !== null) {
        loginField.setText(settings.login);
    }

    // Lookup password field
    passwordField = pane.lookup("#password");
    passwordField.setOnAction(goAuth);
    if (settings.rsaPassword !== null) {
        passwordField.getStyleClass().add("hasSaved");
        passwordField.setPromptText("*** Сохранённый ***");
    }

    // Lookup profiles combobox
    profilesBox = pane.lookup("#profiles");
    profilesBox.setCellFactory(newProfileCell);
    profilesBox.setButtonCell(newProfileCell(null));

    // Lookup save password box
    savePasswordBox = pane.lookup("#savePassword");
    savePasswordBox.setSelected(settings.login === null || settings.rsaPassword !== null);

    // Lookup hyperlink text and actions
    var link = pane.lookup("#link");
    link.setText(config.linkText);
    link.setOnAction(function(event) app.getHostServices().showDocument(config.linkURL.toURI()));

    // Lookup action buttons
    pane.lookup("#goAuth").setOnAction(goAuth);
    pane.lookup("#goSettings").setOnAction(goSettings);
}

function initOffline() {
    // Update title
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

    // Switch news view to offline page
    var offlineURL = Launcher.getResourceURL("dialog/offline/offline.html");
    news.getEngine().load(offlineURL.toString());
}

/* ======== Handler functions ======== */
function goAuth(event) {
    // Verify there's no other overlays
    if (overlay.current !== null) {
        return;
    }

    // Get profile
    var profile = profilesBox.getSelectionModel().getSelectedItem();
    if (profile === null) {
        return; // No profile selected
    }
    else
    {
        ClientLauncher.setProfile(profile.object);
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
        if (!password.isEmpty()) {
            rsaPassword = settings.setPassword(password);
        } else if (settings.rsaPassword !== null) {
            rsaPassword = settings.rsaPassword;
        } else {
            return; // No password - no auth, sorry :C
        }

        // Remember or reset password
        settings.rsaPassword = savePasswordBox.isSelected() ? rsaPassword : null;
    }

    // Show auth overlay
    settings.login = login;
    doAuth(profile, login, rsaPassword);
}

function goSettings(event) {
    // Verify there's no other overlays
    if (overlay.current !== null) {
        return;
    }

    // Show settings overlay
    overlay.show(settings.overlay, null);
}

/* ======== Processing functions ======== */
function verifyLauncher(e) {
    processing.resetOverlay();
    overlay.show(processing.overlay, function(event) makeLauncherRequest(function(result) {
        if (result.binary !== null) {
            LauncherRequest.update(Launcher.getConfig(), result);
            return;
        }
        settings.lastSign = result.sign;
        settings.lastProfiles = result.profiles;

        // Init offline if set
        if (settings.offline) {
            initOffline();
        }

        // Update profiles list and hide overlay
        updateProfilesList(result.profiles);
        overlay.hide(0, function() {
            if (cliParams.autoLogin) {
                goAuth(null);
            }
        });
    }));
}

function doAuth(profile, login, rsaPassword) {
    processing.resetOverlay();
    overlay.show(processing.overlay, function(event) makeAuthRequest(login, rsaPassword, function(result)
        doUpdate(profile, result.pp, result.accessToken)
    ));
}

function doUpdate(profile, pp, accessToken) {
    var digest = profile.object.isUpdateFastCheck();

    // Update asset dir
    overlay.swap(0, update.overlay, function(event) {
            update.resetOverlay("Обновление файлов ресурсов");
            var assetDirName = profile.object.block.getEntryValue("assetDir", StringConfigEntryClass);
            var assetDir = settings.updatesDir.resolve(assetDirName);
            var assetMatcher = profile.object.getAssetUpdateMatcher();
            makeUpdateRequest(assetDirName, assetDir, assetMatcher, digest, function(assetHDir) {
                settings.lastHDirs.put(assetDirName, assetHDir);

                // Update client dir
                update.resetOverlay("Обновление файлов клиента");
                var clientDirName = profile.object.block.getEntryValue("dir", StringConfigEntryClass);
                var clientDir = settings.updatesDir.resolve(clientDirName);
                var clientMatcher = profile.object.getClientUpdateMatcher();
                makeUpdateRequest(clientDirName, clientDir, clientMatcher, digest, function(clientHDir) {
                    settings.lastHDirs.put(clientDirName, clientHDir);
                    doLaunchClient(assetDir, assetHDir, clientDir, clientHDir, profile, pp, accessToken);
                });
            });
    });
}

function doLaunchClient(assetDir, assetHDir, clientDir, clientHDir, profile, pp, accessToken) {
    processing.resetOverlay();
    overlay.swap(0, processing.overlay, function(event)
        launchClient(assetHDir, clientHDir, profile, new ClientLauncherParams(settings.lastSign,
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
    // Set profiles items
    profilesBox.setItems(javafx.collections.FXCollections.observableList(profiles));
    for each (var profile in profiles) {
        pingers[profile.object] = new ServerPinger(profile.object.getServerSocketAddress(), profile.object.getVersion());
    }

    // Set profiles selection model
    var sm = profilesBox.getSelectionModel();
    sm.selectedIndexProperty()["addListener(javafx.beans.value.ChangeListener)"](
        function(o, ov, nv) settings.profile = nv); // Store selected profile index

    // Restore selected item
    var i = settings.profile;
    sm.select(i < profiles.size() ? i : 0);
}

function newProfileCell(listView) {
    var statusBox = loadFXML("dialog/profileCell.fxml");

    // Lookup labels
    var title = statusBox.lookup("#profileTitle");
    var status = statusBox.lookup("#serverStatus");
    var statusCircle = title.getGraphic();

    // Create and return new cell
    var cell = new (Java.extend(javafx.scene.control.ListCell))() {
        updateItem: function(item, empty) {
            Java.super(cell).updateItem(item, empty);
            cell.setGraphic(empty ? null : statusBox);
            if (empty) { // No need to update state
                return;
            }

            // Update title and server status
            title.setText(item.object.getTitle());
            pingServer(status, statusCircle, item);
        }
    };
    cell.setText(null);
    return cell;
}

function pingServer(status, statusCircle, profile) {
    setServerStatus(status, statusCircle, javafx.scene.paint.Color.GREY, "...");
    var task = newTask(function() pingers[profile.object].ping());
    task.setOnSucceeded(function(event) {
        var result = task.getValue();
        var color = result.isOverfilled() ? javafx.scene.paint.Color.YELLOW : javafx.scene.paint.Color.GREEN;
        setServerStatus(status, statusCircle, color, java.lang.String.format("%d из %d", result.onlinePlayers, result.maxPlayers));
    });
    task.setOnFailed(function(event) setServerStatus(status, statusCircle, javafx.scene.paint.Color.RED, "Недоступен"));
    startTask(task);
}

function setServerStatus(status, statusCircle, color, description) {
    status.setText(description);
    statusCircle.setFill(color);
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
        news.setDisable(true);
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
                news.setDisable(false);
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

/* ======== Overlay scripts ======== */
launcher.loadScript(Launcher.getResourceURL("dialog/overlay/debug/debug.js"));
launcher.loadScript(Launcher.getResourceURL("dialog/overlay/processing/processing.js"));
launcher.loadScript(Launcher.getResourceURL("dialog/overlay/settings/settings.js"));
launcher.loadScript(Launcher.getResourceURL("dialog/overlay/update/update.js"));
