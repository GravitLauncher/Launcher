var options = {
    file: DirBridge.dir.resolve("options.bin"), // options file
    autoEnter: false, // Client

    /* options and overlay functions */
    load: function() {
        LogHelper.debug("Loading options file");
        try {
            tryWithResources(new HInput(IOHelper.newInput(options.file)), options.read);
        } catch(e) {
            LogHelper.error(e);
            //options.setDefault();
        }
    },

    save: function() {
        LogHelper.debug("Saving options file");
        try {
            tryWithResources(new HOutput(IOHelper.newOutput(options.file)), options.write);
        } catch(e) {
            LogHelper.error(e);
        }
    },

    // Internal functions
    read: function(input) {
        var magic = input.readInt();
        if (magic != config.settingsMagic) {
            throw new java.io.IOException("options magic mismatch: " + java.lang.Integer.toString(magic, 16));
        }
    },

    write: function(output) {
        output.writeInt(config.settingsMagic);
    },

    /* ===================== OVERLAY ===================== */
    count: 0,

    initOverlay: function() {
        options.overlay = loadFXML("dialog/overlay/options/options.fxml");

        // Lookup autoEnter checkbox
        var holder = options.overlay.lookup("#holder");

        // Lookup apply settings button
        holder.lookup("#apply").setOnAction(function(event) overlay.hide(0, null));
    },

};
