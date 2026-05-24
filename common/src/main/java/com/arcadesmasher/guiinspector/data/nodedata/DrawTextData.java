package com.arcadesmasher.guiinspector.data.nodedata;

import com.arcadesmasher.guiinspector.GUIInspector;
import net.minecraft.client.gui.DrawContext;

public final class DrawTextData extends AlternatingDisplayNodeData implements DrawCallData {
	private final int x;
	private final int y;
	private final int width;

	public DrawTextData(String yarnLabel, String yarnSimpleLabel, String mojmapLabel, String mojmapSimpleLabel, Object[] yarnDetails, Object[] mojmapDetails, int x, int y, int width) {
		super(yarnLabel, yarnSimpleLabel, mojmapLabel, mojmapSimpleLabel, yarnDetails, mojmapDetails);
		this.x = x;
		this.y = y;
		this.width = width;
	}

	@Override
	public void drawOutline(DrawContext context) {
		GUIInspector.drawOutline(context, this.x, this.y, this.width, 9);
	}

	public int x() { return x; }

	public int y() { return y; }

	public int width() { return width; }

}
