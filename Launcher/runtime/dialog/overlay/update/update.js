var update = {
    overlay: null, title: null, description: null, progress: null,

    /* State and overlay functions */
    initOverlay: function() {
        update.overlay = loadFXML("dialog/overlay/update/update.fxml");

        // Lookup nodes
        update.title = update.overlay.lookup("#utitle");
        update.description = update.overlay.lookup("#description");
        update.progress = update.overlay.lookup("#progress");
    },

    resetOverlay: function(title) {
        update.title.setText(title);
        update.description.getStyleClass().remove("error");
        update.description.setText("...");
        update.progress.setProgress(-1.0);
    },

    setError: function(e) {
        LogHelper.error(e);

        // Set error description
        update.description.getStyleClass().add("error");
        update.description.setText(e.toString());
    },

    stateCallback: function(task, state) {
        var bps = state.getBps();
        var estimated = state.getEstimatedTime();
        var estimatedSeconds = estimated === null ? 0 : estimated.getSeconds();
        var estimatedHH = (estimatedSeconds / 3600) | 0;
        var estimatedMM = ((estimatedSeconds % 3600) / 60) | 0;
        var estimatedSS = (estimatedSeconds % 60) | 0;
        task.updateMessage(java.lang.String.format(
            "Файл: %s%n" + // File line
            "Загружено (Файл): %.2f / %.2f MiB.%n" + // File downloaded line
            "Загружено (Всего): %.2f / %.2f MiB.%n" + // Total downloaded line
            "%n" +
            "Средняя скорость: %.1f Kbps%n" + // Speed line
            "Примерно осталось: %d:%02d:%02d%n", // Estimated line

            // Formatting
            state.filePath, // File path
            state.getFileDownloadedMiB() + /* Fuck nashorn */ 0.0, state.getFileSizeMiB() + 0.0, // File downloaded
            state.getTotalDownloadedMiB() + /* Fuck nashorn */ 0.0, state.getTotalSizeMiB() + 0.0, // Total downloaded
            bps <= 0.0 ? 0.0 : bps / 1024.0, // Speed
            estimatedHH, estimatedMM, estimatedSS // Estimated (hh:mm:ss)
        ));
        task.updateProgress(state.totalDownloaded, state.totalSize);
    },

    setTaskProperties: function(task, request, callback) {
        update.description.textProperty().bind(task.messageProperty());
        update.progress.progressProperty().bind(task.progressProperty());
        request.setStateCallback(function(state) update.stateCallback(task, state));
        task.setOnFailed(function(event) {
            update.description.textProperty().unbind();
            update.progress.progressProperty().unbind();
            update.setError(task.getException());
            overlay.hide(2500, null);
        });
        task.setOnSucceeded(function(event) {
            update.description.textProperty().unbind();
            update.progress.progressProperty().unbind();
            if (callback !== null) {
                callback(task.getValue());
            }
        });
    }
};

function offlineUpdateRequest(dirName, dir, matcher, digest) {
    return function() {
        var hdir = settings.lastHDirs.get(dirName);
        if (hdir === null) {
            Request.requestError(java.lang.String.format("Директории '%s' нет в кэше", dirName));
            return;
        }

        // Verify dir with matcher using ClientLauncher's API
        ClientLauncher.verifyHDir(dir, hdir.object, matcher, digest);
        return hdir;
    };
}

/* Export functions */
function makeUpdateRequest(dirName, dir, matcher, digest, callback) {
    var request = settings.offline ? { setStateCallback: function(stateCallback) { /* Ignored */ } } :
        new UpdateRequest(dirName, dir, matcher, digest);
    var task = settings.offline ? newTask(offlineUpdateRequest(dirName, dir, matcher, digest)) :
        newRequestTask(request);

    // Set task properties and start
    update.setTaskProperties(task, request, callback);
    task.updateMessage("Состояние: Хеширование");
    task.updateProgress(-1, -1);
    startTask(task);
}
