package ru.gravit.launcher.gui.choosebox;

import javafx.collections.ObservableList;
import ru.gravit.launcher.LauncherAPI;

public interface IndexedCheckModel<T> extends CheckModel<T> {
	@LauncherAPI
	public void check(int index);

	@LauncherAPI
	public void checkIndices(int... indices);

	@LauncherAPI
	public void clearCheck(int index);

	@LauncherAPI
	public ObservableList<Integer> getCheckedIndices();

	@LauncherAPI
	public T getItem(int index);

	@LauncherAPI
	public int getItemIndex(T item);

	@LauncherAPI
	public boolean isChecked(int index);

	@LauncherAPI
	public void toggleCheckState(int index);

}