var processing = {
    overlay: null, spinner: null, description: null,
    processingImage: null, errorImage: null,

    initOverlay: function() {
        processing.overlay = loadFXML("dialog/overlay/processing/processing.fxml");

        processing.spinner = processing.overlay.lookup("#spinner");
        processing.description = processing.overlay.lookup("#description");
    },

    resetOverlay: function() {
        processing.description.getStyleClass().remove("error");
        processing.description.setText("...");
    },

    setError: function(e) {
        LogHelper.error(e);
        processing.description.textProperty().unbind();
        processing.description.getStyleClass().add("error");
        processing.description.setText(e.toString());
    },

    setTaskProperties: function(task, callback, errorCallback, hide) {
        processing.description.textProperty().bind(task.messageProperty());
        task.setOnFailed(function(event) {
            processing.description.textProperty().unbind();
            processing.setError(task.getException());
            if (hide) {
                overlay.hide(2500, errorCallback);
            } else if (errorCallback !== null) {
                errorCallback();
            }
        });
        task.setOnSucceeded(function(event) {
            processing.description.textProperty().unbind();
            if (callback !== null) {
                callback(task.getValue());
            }
        });
    }
};

function offlineAuthRequest(login) {
    return function() {
        if (!VerifyHelper.isValidUsername(login)) {
            Request.requestError("Имя пользователя некорректно");
            return;
        }
        return {
            pp: PlayerProfile.newOfflineProfile(login),
            accessToken: SecurityHelper.randomStringToken()
        }
    };
}

function makeLauncherRequest(callback) {
    var task = settings.offline ? newTask(FunctionalBridge.offlineLauncherRequest) :
        newRequestTask(new LauncherRequest());

    processing.setTaskProperties(task, callback, function() {
        if (settings.offline) {
            return;
        }

        settings.offline = true;
        overlay.swap(2500, processing.overlay, function() makeLauncherRequest(callback));
    }, false);
    task.updateMessage("Обновление лаунчера");
    startTask(task);
}
function makeProfilesRequest(callback) {
    var task = newRequestTask(new ProfilesRequest());

    processing.setTaskProperties(task, callback, function() {
        if (settings.offline) {
            return;
        }

        settings.offline = true;
        overlay.swap(2500, processing.overlay, function() makeProfilesRequest(callback));
    }, false);
    task.updateMessage("Обновление профилей");
    startTask(task);
}
function makeAuthAvailabilityRequest(callback) {
    var task = newRequestTask(new GetAvailabilityAuthRequest());

    processing.setTaskProperties(task, callback, function() {
        if (settings.offline) {
            return;
        }

        // Repeat request, but in offline mode
        settings.offline = true;
        overlay.swap(2500, processing.overlay, function() makeAuthAvailabilityRequest(callback));
    }, false);
    task.updateMessage("Обновление способов авторизации");
    startTask(task);
}
function makeSetProfileRequest(profile, callback) {
    var task = newRequestTask(new SetProfileRequest(profile));

    processing.setTaskProperties(task, callback, function() {
        if (settings.offline) {
            return;
        }

        // Repeat request, but in offline mode
        settings.offline = true;
        overlay.swap(2500, processing.overlay, function() makeProfilesRequest(callback));
    }, false);
    task.updateMessage("Синхронизация профиля");
    startTask(task);
}

function makeAuthRequest(login, rsaPassword, auth_type, callback) {
    var task = rsaPassword === null ? newTask(offlineAuthRequest(login)) :
        newRequestTask(new AuthRequest(login, rsaPassword, FunctionalBridge.getHWID(), auth_type));
    processing.setTaskProperties(task, callback, null, true);
    task.updateMessage("Авторизация на сервере");
    startTask(task);
}
function makeOAuthRequest(callback) {
    var task = newRequestTask(new OAuthRequest(FunctionalBridge.getHWID()));
    processing.setTaskProperties(task, callback, null, true);
    task.updateMessage("Ожидание авторизация на сервере");
    startTask(task);
}


function launchClient(assetHDir, clientHDir, profile, params, callback) {
    var task = newTask(function() ClientLauncher.launch(assetHDir, clientHDir,
        profile, params, settings.debug));
    processing.setTaskProperties(task, callback, null, true);
    task.updateMessage("Запуск выбранного клиента");
    startTask(task);
}
