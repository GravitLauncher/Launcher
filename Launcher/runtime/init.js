var app, stage, scene, loginScene, menuScene;
var rootPane, loginPane, authPane, menuPane;

// Override application class
var LauncherApp = Java.extend(JSApplication, {
    init: function() {
        app = JSApplication.getInstance();
        cliParams.init(app.getParameters());
        settings.load();
        cliParams.applySettings();
    }, start: function(primaryStage) {
        stage = primaryStage;
		stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        stage.setResizable(false);
        stage.setTitle(config.title);

        // Set icons
        config.icons.forEach(function(icon) {
            var iconURL = Launcher.getResourceURL(icon).toString();
            stage.getIcons().add(new javafx.scene.image.Image(iconURL));
        });

        // Load launcher FXML
        loginPane = loadFXML("dialog/login.fxml");
        menuPane = loadFXML("dialog/mainmenu.fxml");

        loginScene = new javafx.scene.Scene(loginPane);
        loginScene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        menuScene = new javafx.scene.Scene(menuPane);
        menuScene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        setCurrentScene(loginScene);

        initLauncher();

    }, stop: function() {
        settings.save();
        options.save();
    }
});

// Helper functions
function loadFXML(name) {
    var loader = new javafx.fxml.FXMLLoader(Launcher.getResourceURL(name));
    loader.setCharset(IOHelper.UNICODE_CHARSET);
    return loader.load();
}

function setCurrentScene(scene) {
    stage.setScene(scene);
    rootPane = scene.getRoot();
    dimPane = rootPane.lookup("#mask");
    stage.sizeToScene();
    stage.show();
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
launcher.loadScript("dialog/dialog.js");
