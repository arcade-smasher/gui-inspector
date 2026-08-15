package com.arcadesmasher.guiinspector.data.nodedata;

import com.arcadesmasher.guiinspector.GUIInspector;
import net.minecraft.client.gui.GuiGraphics;

public final class DrawItemData extends com.arcadesmasher.guiinspector.data.nodedata.AlternatingDisplayNodeData implements com.arcadesmasher.guiinspector.data.nodedata.DrawCallData {
	private final int x;
	private final int y;

	public DrawItemData(String yarnDisplay, String yarnAltDisplay, String mojmapDisplay, String mojmapAltDisplay, Object[] yarnDetails, Object[] mojmapDetails, int x, int y) {
		super(yarnDisplay, yarnAltDisplay, mojmapDisplay, mojmapAltDisplay, yarnDetails, mojmapDetails);
		this.x = x;
		this.y = y;
	}

	@Override
	public void drawOutline(GuiGraphics context) {
		GUIInspector.drawOutline(context, this.x, this.y, 16, 16);
	}

	public int x() { return x; }

	public int y() { return y; }

}
