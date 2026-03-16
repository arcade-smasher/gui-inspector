package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.minecraft.text.Text;

// supposed to not be abstract, this shouldn't cause issues though
public abstract class FlatButtonWidget extends AbstractWidget implements net.minecraft.client.gui.Drawable {

	private boolean drawBackground; // not final, again shouldn't cause issues
	private boolean drawFrame; // same deal
	private Text label;

	private boolean selected;
	private boolean enabled;
	private boolean visible;

	public abstract boolean isVisible(); // shouldn't be abstract, it's fine

	public abstract void setSelected(boolean selected); // same deal
	public abstract void setEnabled(boolean enabled); // same deal
	public abstract void setVisible(boolean visible); // same deal
}
