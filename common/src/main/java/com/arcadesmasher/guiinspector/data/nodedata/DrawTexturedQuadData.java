package com.arcadesmasher.guiinspector.data.nodedata;

import com.arcadesmasher.guiinspector.GUIInspector;
import net.minecraft.client.gui.DrawContext;

public final class DrawTexturedQuadData extends AlternatingDisplayNodeData implements DrawCallData {
	private final int x1;
	private final int y1;
	private final int x2;
	private final int y2;

	public DrawTexturedQuadData(String display, String altDisplay, Object[] details, int x1, int y1, int x2, int y2) {
		super(display, altDisplay, details);
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
	}

	@Override
	public void drawOutline(DrawContext context) {
		GUIInspector.drawOutline(context, this.x1, this.y1, this.x2 - this.x1, this.y2 - this.y1);
	}

	public int x1() { return x1; }

	public int y1() { return y1; }

	public int x2() { return x2; }

	public int y2() { return y2; }
}
