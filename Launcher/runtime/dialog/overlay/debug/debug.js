var debug = {
    overlay: null,
    output: null,
    action: null,
    process: null,

    initOverlay: function() {
        debug.overlay = loadFXML("dialog/overlay/debug/debug.fxml");

        debug.overlay.lookup("#version").setText(
            java.lang.String.format(
                "%s | Java %s x%s",
                FunctionalBridge.getLauncherVersion(),
                java.lang.System.getProperty("java.version"),
                JVMHelper.JVM_BITS
            )
        );

        debug.output = debug.overlay.lookup("#output");
        debug.output.setEditable(false);

        debug.copy = debug.overlay.lookup("#copy");
        debug.copy.setOnAction(function(event) {
            var haste = FunctionalBridge.hastebin(config.hasteserver, debug.output.getText());

            if (haste == null) {
                debug.copy.setText("Ошибка!");
                return;
            }

            try {
                openURL(new java.net.URL(haste));
            } catch (e) {
                LogHelper.error("Error Open Link");
                LogHelper.error(e);
            }

            var content = new javafx.scene.input.ClipboardContent();
            content.putString(haste);

            javafx.scene.input.Clipboard.getSystemClipboard().
            setContent(content);
        });

        debug.action = debug.overlay.lookup("#action");
        debug.action.setOnAction(function(event) {
            var process = debug.process;
            if (process !== null && process.isAlive()) {
                process.destroyForcibly();
                debug.updateActionButton(true);
                return;
            }

            overlay.hide(0, null);
        });
    },

    resetOverlay: function() {
        debug.output.clear();
        debug.action.setText("");
        debug.action.getStyleClass().remove("kill");
        debug.action.getStyleClass().add("close");
    },

    append: function(text) {
        //Experimental Feature
        if (debug.output.getText().length() > 32000 /* Max length */ ) {
            debug.output.deleteText(0, text.length());
        }
        debug.output.appendText(text);
    },

    updateActionButton: function(forceClose) {
        var process = debug.process;
        var alive = !forceClose &&
            process !== null && process.isAlive();

        var text = alive ? "Убить" : "Закрыть";
        var addClass = alive ? "kill" : "close";
        var removeClass = alive ? "close" : "kill";

        debug.action.setText(text);
        debug.action.getStyleClass().remove(removeClass);
        debug.action.getStyleClass().add(addClass);
    }
};

/* Export functions */
function debugProcess(process) {
    debug.process = process;
    debug.updateActionButton(false);

    var task = newTask(function() {
        var buffer = IOHelper.newCharBuffer();
        var reader = IOHelper.newReader(process.getInputStream(),
            java.nio.charset.Charset.defaultCharset());
        var appendFunction = function(line)
        javafx.application.Platform.runLater(function() debug.append(line));
        for (var length = reader.read(buffer); length >= 0; length = reader.read(buffer)) {
            appendFunction(new java.lang.String(buffer, 0, length));
        }

        return process.waitFor();
    });

    task.setOnFailed(function(event) {
        debug.updateActionButton(true);
        debug.append(java.lang.System.lineSeparator() + task.getException());
    });
    task.setOnSucceeded(function(event) {
        debug.updateActionButton(false);
        debug.append(java.lang.System.lineSeparator() + "Exit code " + task.getValue());
    });

    startTask(task);
}