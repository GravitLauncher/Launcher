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
                var modType = OptionalFile.readType(input);
                var modFile = input.readString(0);
                if(mark)
                {
                    profile.markOptional(modFile,modType);
                    LogHelper.debug("Load options %s marked",modFile);
                }
                else
                {
                    profile.unmarkOptional(modFile,modType);
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
                modFile.writeType(output);
                output.writeString(modFile.name, 0);
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
            var profile = profilesList[serverHolder.old];
            var list = profile.getOptional();
            var checkBoxList = new java.util.ArrayList;
            list.forEach(function(modFile) {
                        var modName = modFile.name, modDescription = "", subLevel = 1;
                        if(!modFile.visible)
                        {
                            LogHelper.debug("optionalMod %s hidden",modFile.name);
                            return;
                        }

                        if(modFile.permissions != 0 && ((loginData.permissions.toLong() & modFile.permissions) != 0))
                        {
                            LogHelper.debug("optionalMod %s permissions deny",modFile.name);
                            return;
                        }
                        if(modFile.info != null) //Есть ли описание?
                            modDescription = modFile.info;
                        if(modFile.subTreeLevel != null && modFile.subTreeLevel > 1)//Это суб-модификация?
                            subLevel = modFile.subTreeLevel;
                         var testMod = new javafx.scene.control.CheckBox(modName);

                        if(subLevel > 1)
                            for(var i = 1; i < subLevel; i++)//Выделение субмодификаций сдвигом.
                                testMod.setTranslateX(25*i);

                         testMod.setSelected(modFile.mark);
                         testMod.setOnAction(function(event) {
                             var isSelected = event.getSource().isSelected();
                             if(isSelected)
                             {
                                 profile.markOptional(modFile);
                                 LogHelper.debug("Selected mod %s", modFile.name);
                             }
                             else
                             {
                                 profile.unmarkOptional(modFile);
                                 LogHelper.debug("Unselected mod %s", modFile.name);
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
                });
            holder.getChildren().clear();
            holder.getChildren().addAll(checkBoxList);
    }

};
