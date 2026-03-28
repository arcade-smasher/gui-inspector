package com.arcadesmasher.guiinspector.data.nodedata;

import net.minecraft.client.gui.DrawContext;

public interface DrawCallData extends NodeData {

	void drawOutline(DrawContext context);
}
