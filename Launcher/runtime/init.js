var app, stage, scene;

// internal
function getPathDirHelper() {
	return dir;
}

// Override application class
var LauncherApp = Java.extend(JSApplication, {
    init: function() {
        app = JSApplication.getInstance();
        cliParams.init(app.getParameters());
        settings.load();
    }, start: function(primaryStage) {
        stage = primaryStage;
        stage.setTitle(config.title);

        // Set icons
        for each (var icon in config.icons) {
            var iconURL = Launcher.getResourceURL(icon).toString();
            stage.getIcons().add(new javafx.scene.image.Image(iconURL));
        }

        // Load dialog FXML
        rootPane = loadFXML("dialog/dialog.fxml");
        initDialog();

        // Set scene
        scene = new javafx.scene.Scene(rootPane);
        stage.setScene(scene);

        // Center and show stage
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.show();
    }, stop: function() {
        settings.save();
    }
});

// Helper functions
function loadFXML(name) {
    var loader = new javafx.fxml.FXMLLoader(Launcher.getResourceURL(name));
    loader.setCharset(IOHelper.UNICODE_CHARSET);
    return loader.load();
}

function setRootParent(parent) {
    scene.setRoot(parent);
}

// Start function - there all begins
function start(args) {
    // Set font rendering properties
    LogHelper.debug("Setting FX properties");
    java.lang.System.setProperty("prism.lcdtext", "false");
    // Start laucher JavaFX stage
    LogHelper.debug("Launching JavaFX application");
    javafx.application.Application.launch(LauncherApp.class, args);
}
