var options = {
    file: DirBridge.dir.resolve("options.bin"), // options file

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
		var holder = options.overlay.lookup("#holder");
        holder.lookup("#apply").setOnAction(function(event) overlay.hide(0, null));
    },

};
function updateOptional()
{
    var holder = options.overlay.lookup("#modlist");
    var nodelist = new java.util.ArrayList;
    
    holder.getChildren().forEach(function(node,i,arr) {
        if(node instanceof javafx.scene.control.CheckBox)
            nodelist.add(node);
    });
    nodelist.forEach(function(node,i,arr) {
        holder.getChildren().remove(node);
    });
    var profile = profilesList[serverHolder.old].object;
    var list = profile.getOptional();
    var checkboxlist = new java.util.ArrayList;
    list.forEach(function(modfile,i,arr) {
         var testMod = new javafx.scene.control.CheckBox(modfile.string);

         testMod.setSelected(modfile.mark);
         //testMod.setLayoutY(2+3*i);
         //testMod.setLayoutX(2);
         testMod.setOnAction(function(event) {
             var isSelected = event.getSource().isSelected();
             if(isSelected)
             {
                 profile.markOptional(modfile.string);
                 LogHelper.debug("Selected mod %s", modfile.string);
             }
             else
             {
                 profile.unmarkOptional(modfile.string);
                 LogHelper.debug("Unselected mod %s", modfile.string);
             }
         });
         checkboxlist.add(testMod);
    });
    holder.getChildren().addAll(checkboxlist);
}