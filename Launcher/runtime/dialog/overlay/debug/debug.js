var debug = {
    overlay: null, output: null, action: null, process: null,

    initOverlay: function() {
        debug.overlay = loadFXML("dialog/overlay/debug/debug.fxml");

        debug.output = debug.overlay.lookup("#output");
        debug.output.setEditable(false);

        debug.copy = debug.overlay.lookup("#copy");
        debug.copy.setOnAction(function(event) {
            var content = new javafx.scene.input.ClipboardContent();
            content.putString(debug.output.getText());

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
