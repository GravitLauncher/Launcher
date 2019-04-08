var app, stage, scene, loginScene, menuScene, consoleScene, optionsScene;
var rootPane, loginPane, menuPane, consolePane, optionsMenu;

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

        config.icons.forEach(function(icon) {
            var iconURL = Launcher.getResourceURL(icon).toString();
            stage.getIcons().add(new javafx.scene.image.Image(iconURL));
        });

        loginPane = loadFXML("dialog/login.fxml");
        menuPane = loadFXML("dialog/mainmenu.fxml");
        consolePane = loadFXML("dialog/console.fxml");
        optionsMenu = loadFXML("dialog/overlay/options/options.fxml");

        loginScene = new javafx.scene.Scene(loginPane);
        loginScene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        menuScene = new javafx.scene.Scene(menuPane);
        menuScene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        consoleScene = new javafx.scene.Scene(consolePane);
        consoleScene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        optionsScene = new javafx.scene.Scene(optionsMenu);
        optionsScene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        setCurrentScene(loginScene);
        initLauncher();

    }, stop: function() {
        settings.save();
        options.save();
    }
});

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

function start(args) {

    LogHelper.debug("Setting FX properties");
    java.lang.System.setProperty("prism.lcdtext", "false");

    LogHelper.debug("Launching JavaFX application");
    javafx.application.Application.launch(LauncherApp.class, args);
}

launcher.loadScript("dialog/dialog.js");
