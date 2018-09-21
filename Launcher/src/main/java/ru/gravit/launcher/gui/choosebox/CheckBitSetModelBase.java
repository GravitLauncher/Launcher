package ru.gravit.launcher.gui.choosebox;

import java.util.BitSet;
import java.util.Map;

import com.sun.javafx.collections.MappingChange;
import com.sun.javafx.collections.NonIterableChange;
import com.sun.javafx.scene.control.ReadOnlyUnbackedObservableList;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import ru.gravit.launcher.LauncherAPI;

abstract class CheckBitSetModelBase<T> implements IndexedCheckModel<T> {
	private final Map<T, BooleanProperty> itemBooleanMap;

	private final BitSet checkedIndices;
	private final ReadOnlyUnbackedObservableList<Integer> checkedIndicesList;
	private final ReadOnlyUnbackedObservableList<T> checkedItemsList;

	CheckBitSetModelBase(final Map<T, BooleanProperty> itemBooleanMap) {
		this.itemBooleanMap = itemBooleanMap;

		this.checkedIndices = new BitSet();

		this.checkedIndicesList = new ReadOnlyUnbackedObservableList<Integer>() {
			@Override
			public boolean contains(Object o) {
				if (o instanceof Number) {
					Number n = (Number) o;
					int index = n.intValue();

					return index >= 0 && index < checkedIndices.length() && checkedIndices.get(index);
				}

				return false;
			}

			@Override
			public Integer get(int index) {
				if (index < 0 || index >= getItemCount())
					return -1;

				for (int pos = 0, val = checkedIndices.nextSetBit(0); val >= 0
						|| pos == index; pos++, val = checkedIndices.nextSetBit(val + 1))
					if (pos == index)
						return val;

				return -1;
			}

			@Override
			public int size() {
				return checkedIndices.cardinality();
			}
		};

		this.checkedItemsList = new ReadOnlyUnbackedObservableList<T>() {
			@Override
			public T get(int i) {
				int pos = checkedIndicesList.get(i);
				if (pos < 0 || pos >= getItemCount())
					return null;
				return getItem(pos);
			}

			@Override
			public int size() {
				return checkedIndices.cardinality();
			}
		};

		final MappingChange.Map<Integer, T> map = this::getItem;

		checkedIndicesList.addListener((ListChangeListener<Integer>) c -> {
			boolean hasRealChangeOccurred = false;
			while (c.next() && !hasRealChangeOccurred)
				hasRealChangeOccurred = c.wasAdded() || c.wasRemoved();

			if (hasRealChangeOccurred) {
				c.reset();
				checkedItemsList.callObservers(new MappingChange<>(c, map, checkedItemsList));
			}
			c.reset();
		});
		getCheckedItems().addListener((ListChangeListener<T>) c -> {
			while (c.next()) {
				if (c.wasAdded())
					for (T item : c.getAddedSubList()) {
						BooleanProperty p = getItemBooleanProperty(item);
						if (p != null)
							p.set(true);
					}

				if (c.wasRemoved())
					for (T item : c.getRemoved()) {
						BooleanProperty p = getItemBooleanProperty(item);
						if (p != null)
							p.set(false);
					}
			}
		});
	}
	@LauncherAPI
	@Override
	public void check(int index) {
		if (index < 0 || index >= getItemCount())
			return;
		checkedIndices.set(index);
		final int changeIndex = checkedIndicesList.indexOf(index);
		checkedIndicesList.callObservers(
				new NonIterableChange.SimpleAddChange<>(changeIndex, changeIndex + 1, checkedIndicesList));
	}
	@Override
	public void check(T item) {
		int index = getItemIndex(item);
		check(index);
	}
	@LauncherAPI
	@Override
	public void checkAll() {
		for (int i = 0; i < getItemCount(); i++)
			check(i);
	}
	@LauncherAPI
	@Override
	public void checkIndices(int... indices) {
		for (int indice : indices)
			check(indice);
	}
	@LauncherAPI
	@Override
	public void clearCheck(int index) {
		if (index < 0 || index >= getItemCount())
			return;
		checkedIndices.clear(index);

		final int changeIndex = checkedIndicesList.indexOf(index);
		checkedIndicesList.callObservers(
				new NonIterableChange.SimpleRemovedChange<>(changeIndex, changeIndex, index, checkedIndicesList));
	}
	@LauncherAPI
	@Override
	public void clearCheck(T item) {
		int index = getItemIndex(item);
		clearCheck(index);
	}
	@LauncherAPI
	@Override
	public void clearChecks() {
		for (int index = 0; index < checkedIndices.length(); index++)
			clearCheck(index);
	}
	@LauncherAPI
	@Override
	public ObservableList<Integer> getCheckedIndices() {
		return checkedIndicesList;
	}
	@LauncherAPI
	@Override
	public ObservableList<T> getCheckedItems() {
		return checkedItemsList;
	}
	@LauncherAPI
	@Override
	public abstract T getItem(int index);
	@LauncherAPI
	BooleanProperty getItemBooleanProperty(T item) {
		return itemBooleanMap.get(item);
	}
	@LauncherAPI
	@Override
	public abstract int getItemCount();
	@LauncherAPI
	@Override
	public abstract int getItemIndex(T item);
	@LauncherAPI
	@Override
	public boolean isChecked(int index) {
		return checkedIndices.get(index);
	}
	@LauncherAPI
	@Override
	public boolean isChecked(T item) {
		int index = getItemIndex(item);
		return isChecked(index);
	}
	@LauncherAPI
	@Override
	public boolean isEmpty() {
		return checkedIndices.isEmpty();
	}
	@LauncherAPI
	@Override
	public void toggleCheckState(int index) {
		if (isChecked(index))
			clearCheck(index);
		else
			check(index);
	}

	@LauncherAPI
	@Override
	public void toggleCheckState(T item) {
		int index = getItemIndex(item);
		toggleCheckState(index);
	}
	@LauncherAPI
	protected void updateMap() {
		itemBooleanMap.clear();
		for (int i = 0; i < getItemCount(); i++) {
			final int index = i;
			final T item = getItem(index);

			final BooleanProperty booleanProperty = new SimpleBooleanProperty(item, "selected", false); //$NON-NLS-1$
			itemBooleanMap.put(item, booleanProperty);

			booleanProperty.addListener(o -> {
				if (booleanProperty.get()) {
					checkedIndices.set(index);
					final int changeIndex1 = checkedIndicesList.indexOf(index);
					checkedIndicesList.callObservers(new NonIterableChange.SimpleAddChange<>(changeIndex1,
							changeIndex1 + 1, checkedIndicesList));
				} else {
					final int changeIndex2 = checkedIndicesList.indexOf(index);
					checkedIndices.clear(index);
					checkedIndicesList.callObservers(new NonIterableChange.SimpleRemovedChange<>(changeIndex2,
							changeIndex2, index, checkedIndicesList));
				}
			});
		}
	}
}