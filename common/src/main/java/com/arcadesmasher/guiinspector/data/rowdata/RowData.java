package com.arcadesmasher.guiinspector.data.rowdata;

import com.arcadesmasher.guiinspector.data.FriendlyDisplay;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface RowData<T> extends FriendlyDisplay {

	Class<T> type();

	Supplier<T> getter();

	Consumer<T> setter();

	String label();
}
