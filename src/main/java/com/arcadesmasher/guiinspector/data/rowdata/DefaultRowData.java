package com.arcadesmasher.guiinspector.data.rowdata;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class DefaultRowData<T> implements RowData<T> {

	private final Supplier<T> getter;
	private final Consumer<T> setter;
	private final String label;
	private final Class<T> type;

	public DefaultRowData(Supplier<T> getter, Consumer<T> setter, String label, Class<T> type) {
		this.getter = getter;
		this.setter = setter;
		this.label = label;
		this.type = type;
	}

	@Override
	public Class<T> getType() { return type; }

	@Override public Supplier<T> getter() { return getter; }
	@Override public Consumer<T> setter() { return setter; }

	@Override
	public String label() { return label; }

	@Override
	public String display() { return label + getter.get(); }
}