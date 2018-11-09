package ru.gravit.launcher.gui.choosebox;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Skin;
import javafx.util.StringConverter;

import ru.gravit.launcher.LauncherAPI;

public class CheckComboBox<T> extends ControlsFXControl {
    private static class CheckComboBoxBitSetCheckModel<T> extends CheckBitSetModelBase<T> {
        private final ObservableList<T> items;
        
        CheckComboBoxBitSetCheckModel(final ObservableList<T> items, final Map<T, BooleanProperty> itemBooleanMap) {
            super(itemBooleanMap);

            this.items = items;
            this.items.addListener((ListChangeListener<T>) c -> updateMap());

            updateMap();
        }

        @Override
        public T getItem(int index) {
            return items.get(index);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public int getItemIndex(T item) {
            return items.indexOf(item);
        }
    }

    private final ObservableList<T> items;
    private final Map<T, BooleanProperty> itemBooleanMap;
    private CheckComboBoxSkin<T> checkComboBoxSkin;
    private ObjectProperty<IndexedCheckModel<T>> checkModel = new SimpleObjectProperty<>(this, "checkModel");
    private ObjectProperty<StringConverter<T>> converter = new SimpleObjectProperty<>(this,
            "converter");
    private StringProperty title = new SimpleStringProperty(null);

    public CheckComboBox() {
        this(null);
    }

    public CheckComboBox(final ObservableList<T> items) {
        final int initialSize = items == null ? 32 : items.size();

        this.itemBooleanMap = new HashMap<>(initialSize);
        this.items = items == null ? FXCollections.observableArrayList() : items;
        setCheckModel(new CheckComboBoxBitSetCheckModel<>(this.items, itemBooleanMap));
    }

    @LauncherAPI
    public final ObjectProperty<IndexedCheckModel<T>> checkModelProperty() {
        return checkModel;
    }

    @LauncherAPI
    public final ObjectProperty<StringConverter<T>> converterProperty() {
        return converter;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        checkComboBoxSkin = new CheckComboBoxSkin<>(this);
        return checkComboBoxSkin;
    }

    @LauncherAPI
    public final IndexedCheckModel<T> getCheckModel() {
        return checkModel == null ? null : checkModel.get();
    }

    @LauncherAPI
    public final StringConverter<T> getConverter() {
        return converterProperty().get();
    }

    @LauncherAPI
    public BooleanProperty getItemBooleanProperty(int index) {
        if (index < 0 || index >= items.size())
            return null;
        return getItemBooleanProperty(getItems().get(index));
    }

    @LauncherAPI
    public BooleanProperty getItemBooleanProperty(T item) {
        return itemBooleanMap.get(item);
    }

    @LauncherAPI
    public ObservableList<T> getItems() {
        return items;
    }

    @LauncherAPI
    public final String getTitle() {
        return title.getValue();
    }

    @LauncherAPI
    public void hide() {
        if (checkComboBoxSkin != null)
            checkComboBoxSkin.hide();
    }

    @LauncherAPI
    public final void setCheckModel(IndexedCheckModel<T> value) {
        checkModelProperty().set(value);
    }

    @LauncherAPI
    public final void setConverter(StringConverter<T> value) {
        converterProperty().set(value);
    }

    @LauncherAPI
    public final void setTitle(String value) {
        title.setValue(value);
    }

    @LauncherAPI
    public void show() {
        if (checkComboBoxSkin != null)
            checkComboBoxSkin.show();
    }

    @LauncherAPI
    public final StringProperty titleProperty() {
        return title;
    }
}
