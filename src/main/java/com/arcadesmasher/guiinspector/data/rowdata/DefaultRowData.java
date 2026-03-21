package com.arcadesmasher.guiinspector.data.rowdata;

import java.util.function.Consumer;
import java.util.function.Supplier;

public record DefaultRowData<T>(Supplier<T> getter, Consumer<T> setter, String label, Class<T> type) implements RowData<T> {

	@Override
	public String display() {
		return label + getter.get();
	}
}