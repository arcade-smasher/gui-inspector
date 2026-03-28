package com.arcadesmasher.guiinspector;

import com.arcadesmasher.guiinspector.tree.TreePanel;

public interface SodiumCompat {

	void addSodiumEntries(TreePanel widgets, Object object);
	boolean isDim2i(Object object);
	boolean hasDimensions(Object object);
	boolean isCenteredFlatWidget(Object object);
	int[] getDimensions(Object object);
}
