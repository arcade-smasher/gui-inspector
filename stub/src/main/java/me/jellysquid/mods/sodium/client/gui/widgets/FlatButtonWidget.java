package me.jellysquid.mods.sodium.client.gui.widgets;

import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.text.Text;

// supposed to not be abstract, this shouldn't cause issues though
public abstract class FlatButtonWidget extends AbstractWidget implements net.minecraft.client.gui.Drawable {

	private Dim2i dim; // not final, again shouldn't cause issues

	private boolean selected;
	private boolean enabled;
	private boolean visible;

	private Text label;

	public abstract void setLabel(Text text); // same deal
	public abstract Text getLabel(); // same deal

	public abstract void setSelected(boolean selected); // same deal
	public abstract void setEnabled(boolean enabled); // same deal
	public abstract void setVisible(boolean visible); // same deal
}
