package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.minecraft.network.chat.Component;

public abstract class CenteredFlatWidget extends AbstractWidget {

	private boolean isSelectable; // not final, won't cause issues though as long as we properly @Mutable

	private boolean selected;
	private boolean enabled;
	private boolean visible;

	private Component label; // not final, same deal
	private Component subtitle; // not final, same deal

	public abstract void setSelected(boolean selected); // not abstract normally, it's fine.
	public abstract void setEnabled(boolean enabled); // same deal
	public abstract void setVisible(boolean visible); // same deal
}