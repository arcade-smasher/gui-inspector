package com.arcadesmasher.guiinspector;

import net.minecraft.text.Text;

public interface FlatButtonWidgetAccessor {
	Text acquireLabel();
	void changeLabel(Text label);

	boolean isSelected();
	void invokeSetSelected(boolean selected);

	boolean isEnabled();
	void invokeSetEnabled(boolean enabled);

	boolean getVisible();
	void invokeSetVisible(boolean visible);
}
