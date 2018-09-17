package ru.gravit.launcher.gui.choosebox;

import javafx.collections.ObservableList;
import ru.gravit.launcher.LauncherAPI;

public interface CheckModel<T> {
	@LauncherAPI
	public void check(T item);

	@LauncherAPI
	public void checkAll();

	@LauncherAPI
	public void clearCheck(T item);

	@LauncherAPI
	public void clearChecks();

	@LauncherAPI
	public ObservableList<T> getCheckedItems();

	@LauncherAPI
	public int getItemCount();

	@LauncherAPI
	public boolean isChecked(T item);

	@LauncherAPI
	public boolean isEmpty();

	@LauncherAPI
	public void toggleCheckState(T item);
}
