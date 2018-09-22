package ru.gravit.launcher.gui.choosebox;

import javafx.collections.ObservableList;
import ru.gravit.launcher.LauncherAPI;

public interface CheckModel<T> {
    @LauncherAPI
    void check(T item);

    @LauncherAPI
    void checkAll();

    @LauncherAPI
    void clearCheck(T item);

    @LauncherAPI
    void clearChecks();

    @LauncherAPI
    ObservableList<T> getCheckedItems();

    @LauncherAPI
    int getItemCount();

    @LauncherAPI
    boolean isChecked(T item);

    @LauncherAPI
    boolean isEmpty();

    @LauncherAPI
    void toggleCheckState(T item);
}
