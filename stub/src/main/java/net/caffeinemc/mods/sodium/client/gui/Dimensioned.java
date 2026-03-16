package net.caffeinemc.mods.sodium.client.gui;

import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface Dimensioned {
	Dim2i getDimensions();

	default int getX() { return this.getDimensions().x(); }
	default int getY() { return this.getDimensions().y(); }
	default int getWidth() { return this.getDimensions().width(); }
	default int getHeight() { return this.getDimensions().height(); }
}