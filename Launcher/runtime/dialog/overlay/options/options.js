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
                if(hprofile.getSortIndex() == sortIndex)
                {
                    profile = hprofile;
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
            var profile = hprofile;
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
            
            modConfigKeys.forEach(function(key, id) {//По умолчанию индекс у ветви = 1. Выставляем его у всех неуказанных.
                    if(optModNames.modInfo[key].subTreeLevel == null) 
                        optModNames.modInfo[key].subTreeLevel = 1;
            });

            for (var ik = 0, l = modConfigKeys.length + 1; ik <= l; ik++) {
                list.forEach(function(modFile) {
                    if((modConfigKeys[ik] === modFile.string) || (ik == modConfigKeys.length+1 && dModsIds.indexOf(modFile.string) == -1)) {
                        dModsIds.push(modFile.string);

                        var modName = modFile.string, modDescription = "", subLevel = 1;
                        if(optModNames.modInfo[modFile.string] != null){//Есть ли хоть какое-нибудь представление описания модификации?
                            var optModN = optModNames.modInfo[modFile.string];
                            if(optModN.name != null)//Есть ли у модификации имя?
                                modName = optModN.name;
                            if(optModN.description != null) //Есть ли описание?
                                modDescription = optModN.description;
                            if(optModN.subTreeLevel != null && optModN.subTreeLevel > 1)//Это суб-модификация?
                                subLevel = optModN.subTreeLevel;
                        } else if(optModNames.optAutoModName) {
                            //Попытка автоматически создать представляемое имя модификации.
                            modName = modName.replace(new RegExp("(.*?(\/))",'g'),'');
                            modName = modName.replace(new RegExp("(-|_|[\\d]|\\+).*",'g'),'');
                            //Первая буква - заглавная
                            modName = modName[0].toUpperCase() + modName.slice(1);
                        }
                         var testMod = new javafx.scene.control.CheckBox(modName);

                        if(subLevel > 1)
                            for(var i = 1; i < subLevel; i++)//Выделение субмодификаций сдвигом.
                                testMod.setTranslateX(25*i);

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
                             if(subLevel > 1) {
                                 for(var i = 1; i < subLevel; i++){
                                    textDescr.setWrappingWidth(370-(25*i));
                                    textDescr.setTranslateX(25+(25*i));
                                 }
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
                var modIDs = Object.keys(modList);

                if(modInfo.subTreeLevel != null) {

                    if(modInfo.subTreeLevel >= 1){//Отключение core-модификации
                        var stop = false;
                        modIDs.forEach(function(key, id) {
                            if(modList[key] != null && modList[key].subTreeLevel != null) {
                                if( modList[key].subTreeLevel > modInfo.subTreeLevel && modIDs.indexOf(key) > modIDs.indexOf(ImodFile) && enable == false && stop == false) {
                                    if(options.modExists(key)){
                                        profile.unmarkOptional(key);
                                        LogHelper.debug("Unselected subMod %s", key);
                                    }
                                } else if(modIDs.indexOf(key) > modIDs.indexOf(ImodFile) && modList[key].subTreeLevel <= modInfo.subTreeLevel && stop == false) {
                                    //LogHelper.debug("STOP disable!! " + key);
                                    stop = true;
                                }
                            }
                        });
                    }

                    if(modInfo.onlyOne == true){//Включение onlyOne-модификации (Все onlyOne-модификации с той же группой будут отключены. К примеру 2 миникарты)
                        modIDs.forEach(function(key, id) {
                            if(modList[key] != null && modList[key].onlyOneGroup != null) {
                                if(modList[key].onlyOneGroup == modInfo.onlyOneGroup && modList[key].onlyOne == true && enable == true && key != ImodFile) {
                                    if(options.modExists(key)) {
                                        profile.unmarkOptional(key);
                                        LogHelper.debug("Unselected Mod (onlyOne toggle) %s", key);
                                    }
                                    options.treeToggle(false, key); //И все его подмодификации канут в Лету..
                                }
                            }
                        });
                    }

                    if(modInfo.subTreeLevel > 1){//Включение суб-модификации (Без core суб-моды работать не будут, так что его нужно включать) (Включаем всю ветку зависимости)
                        var reverseModList = Object.keys(modList).reverse();
                        var tsl = modInfo.subTreeLevel-1;
                        reverseModList.forEach(function(key, id) {
                            if(modList[key] != null && modList[key].subTreeLevel != null) {
                                if(modList[key].subTreeLevel == tsl && modIDs.indexOf(key) < modIDs.indexOf(ImodFile) && enable == true) {
                                    if(options.modExists(key)) {
                                        profile.markOptional(key);
                                        LogHelper.debug("Selected coreMod %s", key);
                                    }
                                    options.treeToggle(true, key); //Для срабатывания onlyOne-модификаций.
                                    tsl--;
                                }
                            }
                        });
                    }

                }
            }
    },
    modExists: function(key){
        var profile = profilesList[serverHolder.old].object;
        var list = profile.getOptional();
        var result = false;
        list.forEach(function(modFile) {
            if(modFile.string === key) {
                result = true;
            }
        });
        return result;
    }

};
