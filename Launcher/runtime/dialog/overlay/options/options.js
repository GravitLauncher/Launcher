var options = {
    file: DirBridge.dir.resolve("options.bin"), // options file

    /* options and overlay functions */
    load: function(profiles) {
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
        var profilesCount  = input.readInt();
        LogHelper.debug("Load options. ProfilesCount %d",profilesCount);
        for(var i = 0;i<profilesCount;i++)
        {
            var listSize = input.readInt();
            var sortIndex = input.readInt();
            var profile = null;
            settings.lastProfiles.forEach(function(hprofile,i,arr) {
                if(hprofile.object.getSortIndex() == sortIndex)
                {
                    profile = hprofile.object;
                }
            });
            for(var j = 0; j < listSize; j++)
            {
                var mark = input.readBoolean();
                var modFile = input.readString(0);
                if(mark)
                {
                    profile.markOptional(modFile);
                    LogHelper.debug("Load options %s marked",modFile);
                }
                else
                {
                    profile.unmarkOptional(modFile);
                    LogHelper.debug("Load options %s unmarked",modFile);
                }
            }
        }
    },

    write: function(output) {
        output.writeInt(config.settingsMagic);
        output.writeInt(settings.lastProfiles.length);
        settings.lastProfiles.forEach(function(hprofile,i,arr) {
            var profile = hprofile.object;
            LogHelper.debug("Save options %s",profile.getTitle());
            var list = profile.getOptional();
            output.writeInt(list.size());
            output.writeInt(profile.getSortIndex());
            list.forEach(function(modFile,j,arr2) {
                output.writeBoolean(modFile.mark);
                output.writeString(modFile.string, 0);
            });
        });
    },

    /* ===================== OVERLAY ===================== */
    count: 0,

    initOverlay: function() {
        options.overlay = loadFXML("dialog/overlay/options/options.fxml");
		var holder = options.overlay.lookup("#holder");
        holder.lookup("#apply").setOnAction(function(event) overlay.hide(0, null));
    },
    update: function() {
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
            var checkBoxList = new java.util.ArrayList;
            var modConfigKeys = Object.keys(optModNames.modInfo);
            var dModsIds = [];

            for (var ik = 0, l = modConfigKeys.length + 1; ik <= l; ik++) {
                list.forEach(function(modFile) {
                    if((modConfigKeys[ik] === modFile.string) || (ik == modConfigKeys.length+1 && dModsIds.indexOf(modFile.string) == -1)) {
                        dModsIds.push(modFile.string);

                        var modName = modFile.string, modDescription = "", subm = false;
                        if(optModNames.modInfo[modFile.string] != null){//Есть ли хоть какое-нибудь представление описания модификации?
                            var optModN = optModNames.modInfo[modFile.string];
                            if(optModN.name != null)//Есть ли у модификации имя?
                                modName = optModN.name;
                            if(optModN.description != null) //Есть ли описание?
                                modDescription = optModN.description;
                            if(optModN.subMod != null && optModN.subMod == true)//Это суб-модификация?
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

                         testMod.setSelected(modFile.mark);
                         testMod.setOnAction(function(event) {
                             var isSelected = event.getSource().isSelected();
                             if(isSelected)
                             {
                                 profile.markOptional(modFile.string);
                                 LogHelper.debug("Selected mod %s", modFile.string);
                                 options.treeToggle(true, modFile.string);
                             }
                             else
                             {
                                 profile.unmarkOptional(modFile.string);
                                 LogHelper.debug("Unselected mod %s", modFile.string);
                                 options.treeToggle(false, modFile.string);
                             }
                             options.update();
                         });
                        checkBoxList.add(testMod);

                         if(modDescription != "") { //Добавляем описание?
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
                             checkBoxList.add(textDescr);
                         }
                        sep = new javafx.scene.control.Separator();
                        sep.getStyleClass().add("separator");
                        checkBoxList.add(sep);
                    }
                });
            }
            holder.getChildren().clear();
            holder.getChildren().addAll(checkBoxList);
    },
    treeToggle: function(enable, ImodFile) {
            var profile = profilesList[serverHolder.old].object;
            if(optModNames.modInfo[ImodFile] != null) {
                var modInfo = optModNames.modInfo[ImodFile];
                var modList = optModNames.modInfo;

                if(modInfo.group != null && modInfo.subMod != null) {

                    if(modInfo.subMod == false){//Отключение core-модификации
                        Object.keys(modList).forEach(function(key, id) {
                            if(modList[key] != null && modList[key].group != null && modList[key].subMod != null) {
                                if(modList[key].group == modInfo.group && modList[key].subMod == true && enable == false) {
                                    profile.unmarkOptional(key);
                                    LogHelper.debug("Unselected subMod %s", key);
                                }
                            }
                        });
                    }

                    if(modInfo.subMod == false){//Включение core-модификации (Все core-модификации с той же группой будут отключены. К примеру 2 миникарты)
                        Object.keys(modList).forEach(function(key, id) {
                            if(modList[key] != null && modList[key].group != null) {
                                if(modList[key].group == modInfo.group && modList[key].subMod == false && enable == true && key != ImodFile) {
                                    profile.unmarkOptional(key);
                                    LogHelper.debug("Unselected coreMod %s", key);
                                }
                            }
                        });
                    }

                    if(modInfo.subMod == true){//Включение суб-модификации (Без core суб-моды работать не будут, так что его нужно включать)
                        Object.keys(modList).forEach(function(key, id) {
                            if(modList[key] != null && modList[key].group != null && modList[key].subMod != null) {
                                if(modList[key].group == modInfo.group && modList[key].subMod == false && enable == true) {
                                    profile.markOptional(key);
                                    LogHelper.debug("Selected coreMod %s", key);
                                }
                            }
                        });
                    }

                }
            }
    }

};