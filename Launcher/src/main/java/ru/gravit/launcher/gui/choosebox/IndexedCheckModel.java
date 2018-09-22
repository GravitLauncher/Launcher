package ru.gravit.launcher.gui.choosebox;

import javafx.collections.ObservableList;
import ru.gravit.launcher.LauncherAPI;

public interface IndexedCheckModel<T> extends CheckModel<T> {
    @LauncherAPI
    void check(int index);

    @LauncherAPI
    void checkIndices(int... indices);

    @LauncherAPI
    void clearCheck(int index);

    @LauncherAPI
    ObservableList<Integer> getCheckedIndices();

    @LauncherAPI
    T getItem(int index);

    @LauncherAPI
    int getItemIndex(T item);

    @LauncherAPI
    boolean isChecked(int index);

    @LauncherAPI
    void toggleCheckState(int index);

}