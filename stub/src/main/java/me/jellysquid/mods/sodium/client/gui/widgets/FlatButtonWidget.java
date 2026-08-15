package me.jellysquid.mods.sodium.client.gui.widgets;

import me.jellysquid.mods.sodium.client.util.Dim2i;
import 	net.minecraft.network.chat.Component;

// supposed to not be abstract, this shouldn't cause issues though
public abstract class FlatButtonWidget extends AbstractWidget implements net.minecraft.client.gui.components.Renderable {

	private Dim2i dim; // not final, again shouldn't cause issues

	private boolean selected;
	private boolean enabled;
	private boolean visible;

	private Component label;

	public abstract void setLabel(Component text); // same deal
	public abstract Component getLabel(); // same deal

	public abstract void setSelected(boolean selected); // same deal
	public abstract void setEnabled(boolean enabled); // same deal
	public abstract void setVisible(boolean visible); // same deal
}
