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
var upd = false; //Переменная обноеления интерфейса.
function updateOptional()
{
    var holder = options.overlay.lookup("#modlist").getContent();
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
		var modName = modfile.string, modDescription = "", subm = false;
		if(optModNames.modInfo[modfile.string] != null){//Есть ли хоть какое ни будь представление описания модификации?
			var optModN = optModNames.modInfo[modfile.string];
			if(optModN.name != null)//Есть ли у модификации имя?
				modName = optModN.name;
			if(optModN.description != null) //Есть ли описание?
				modDescription = optModN.description;
			if(optModN.submod != null && optModN.submod == true)//Это суб-модификация?
				subm = true;
		} else if(optModNames.optAutoModName) {
			//Попытка автоматически создать представляемое имя модификации.
			modName = modName.replace(new RegExp("(.*?(\/))",'g'),'');
			modName = modName.replace(new RegExp("(-|_|[\\d]|\\+).*",'g'),'');
			//Первая буква - заглавная
			modName = modName[0].toUpperCase() + modName.slice(1);
		}
         var testMod = new javafx.scene.control.CheckBox(modName);
		 
		 if(subm)//Это суб-модификация?
			 testMod.setTranslateX(25);

         testMod.setSelected(modfile.mark);
         testMod.setOnAction(function(event) {
             var isSelected = event.getSource().isSelected();
             if(isSelected)
             {
                 profile.markOptional(modfile.string);
                 LogHelper.debug("Selected mod %s", modfile.string);
                 optionalModTreeToggle(true, modfile.string);
             }
             else
             {
                 profile.unmarkOptional(modfile.string);
                 LogHelper.debug("Unselected mod %s", modfile.string);
                 optionalModTreeToggle(false, modfile.string);
             }
             upd = true;
             updateOptional();
         });
		checkboxlist.add(testMod);
        
		 if(modDescription != "") { //Добавляем оаисание
			 textDescr = new javafx.scene.text.Text(modDescription);
             if(subm){//Это суб-модификация?
                textDescr.setWrappingWidth(345);
                textDescr.setTranslateX(50);
			 } else {
                textDescr.setWrappingWidth(370);
                textDescr.setTranslateX(25);
			 }
			 textDescr.setTextAlignment(javafx.scene.text.TextAlignment.JUSTIFY);
			 textDescr.getStyleClass().add("description-text");
			 checkboxlist.add(textDescr);
		 }
        sep = new javafx.scene.control.Separator();
        sep.getStyleClass().add("separator");
        checkboxlist.add(sep);
         
    });
    if(upd) holder.getChildren().clear();
    holder.getChildren().addAll(checkboxlist);
};
function optionalModTreeToggle(enable, Imodfile) { //Переключение ветки модов
    var profile = profilesList[serverHolder.old].object;
    if(optModNames.modInfo[Imodfile] != null) {
        var modInfo = optModNames.modInfo[Imodfile];
        var modList = optModNames.modInfo;
        
        if(modInfo.group != null && modInfo.submod != null) {
        
            if(modInfo.submod == false){//Отключение core-модификации
                Object.keys(modList).forEach(function(key, id) {
                    if(modList[key] != null && modList[key].group != null && modList[key].submod != null) {
                        if(modList[key].group == modInfo.group && modList[key].submod == true && enable == false) {
                            profile.unmarkOptional(key);
                            LogHelper.debug("Unselected subMod %s", key);
                        }
                    }
                });
            }
            
            if(modInfo.submod == true){//Включение суб-модификации (Без core суб-моды работать не будут, так что его нужно включать)
                Object.keys(modList).forEach(function(key, id) {
                    if(modList[key] != null && modList[key].group != null && modList[key].submod != null) {
                        if(modList[key].group == modInfo.group && modList[key].submod == false && enable == true) {
                            profile.markOptional(key);
                            LogHelper.debug("Selected coreMod %s", key);
                        }
                    }
                });
            }
            
        }
    }
}
