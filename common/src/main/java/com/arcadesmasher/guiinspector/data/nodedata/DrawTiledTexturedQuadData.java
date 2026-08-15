package com.arcadesmasher.guiinspector.data.nodedata;

import com.arcadesmasher.guiinspector.GUIInspector;
import net.minecraft.client.gui.GuiGraphics;

public final class DrawTiledTexturedQuadData extends com.arcadesmasher.guiinspector.data.nodedata.AlternatingDisplayNodeData implements com.arcadesmasher.guiinspector.data.nodedata.DrawCallData {
	private final int x0;
	private final int y0;
	private final int x1;
	private final int y1;

	public DrawTiledTexturedQuadData(String yarnLabel, String yarnSimpleLabel, String mojmapLabel, String mojmapSimpleLabel, Object[] yarnDetails, Object[] mojmapDetails, int x0, int y0, int x1, int y1) {
		super(yarnLabel, yarnSimpleLabel, mojmapLabel, mojmapSimpleLabel, yarnDetails, mojmapDetails);
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
	}

	@Override
	public void drawOutline(GuiGraphics context) {
		GUIInspector.drawOutline(context, this.x0, this.y0, this.x1 - this.x0, this.y1 - this.y0);
	}

	public int x0() { return x0; }

	public int y0() { return y0; }

	public int x1() { return x1; }

	public int y1() { return y1; }

}
