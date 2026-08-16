package com.arcadesmasher.guiinspector.data.nodedata;

import com.arcadesmasher.guiinspector.GUIInspector;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class DrawTextData extends com.arcadesmasher.guiinspector.data.nodedata.AlternatingDisplayNodeData implements com.arcadesmasher.guiinspector.data.nodedata.DrawCallData {
	private final int x;
	private final int y;
	private final int width;

	public DrawTextData(String label, String simpleLabel, Object[] details, int x, int y, int width) {
		super(label, simpleLabel, details);
		this.x = x;
		this.y = y;
		this.width = width;
	}

	@Override
	public void drawOutline(GuiGraphicsExtractor context) {
		GUIInspector.drawOutline(context, this.x, this.y, this.width, 9);
	}

	public int x() { return x; }

	public int y() { return y; }

	public int width() { return width; }

}
