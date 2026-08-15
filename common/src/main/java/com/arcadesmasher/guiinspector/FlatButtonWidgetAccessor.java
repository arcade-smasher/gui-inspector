package com.arcadesmasher.guiinspector;

import net.minecraft.network.chat.Component;

public interface FlatButtonWidgetAccessor {
	Component acquireLabel();
	void changeLabel(Component label);

	boolean isSelected();
	void invokeSetSelected(boolean selected);

	boolean isEnabled();
	void invokeSetEnabled(boolean enabled);

	boolean getVisible();
	void invokeSetVisible(boolean visible);
}
