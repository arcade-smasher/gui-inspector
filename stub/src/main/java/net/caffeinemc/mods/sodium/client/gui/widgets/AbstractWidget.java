package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class AbstractWidget implements net.minecraft.client.gui.Drawable, net.minecraft.client.gui.Element, net.minecraft.client.gui.Selectable, net.caffeinemc.mods.sodium.client.gui.Dimensioned {

	protected boolean hovered;

	public abstract boolean isHovered(); // normal method is not abstract. shouldn't be a problem though
}