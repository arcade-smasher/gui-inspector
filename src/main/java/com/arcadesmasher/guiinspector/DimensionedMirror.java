package com.arcadesmasher.guiinspector;

import net.caffeinemc.mods.sodium.client.util.Dim2i;

public interface DimensionedMirror {
	int getX();
	int getY();
	int getWidth();
	int getHeight();
	Dim2i getDim2i();
}
