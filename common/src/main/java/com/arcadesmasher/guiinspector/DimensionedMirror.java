package com.arcadesmasher.guiinspector;

import com.arcadesmasher.guiinspector.Dim2iAccessor;

public interface DimensionedMirror {
	int getX();
	int getY();
	int getWidth();
	int getHeight();
	com.arcadesmasher.guiinspector.Dim2iAccessor getDim2i();
}
