package com.arcadesmasher.guiinspector.data.nodedata;

import com.arcadesmasher.guiinspector.GUIInspector;
import net.minecraft.client.gui.DrawContext;

public final class DrawTiledTexturedQuadData extends AlternatingDisplayNodeData implements DrawCallData {
	private final int x0;
	private final int y0;
	private final int x1;
	private final int y1;

	public DrawTiledTexturedQuadData(String display, String altDisplay, Object[] details, int x0, int y0, int x1, int y1) {
		super(display, altDisplay, details);
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
	}

	@Override
	public void drawOutline(DrawContext context) {
		GUIInspector.drawOutline(context, this.x0, this.y0, this.x1 - this.x0, this.y1 - this.y0);
	}

	public int x0() { return x0; }

	public int y0() { return y0; }

	public int x1() { return x1; }

	public int y1() { return y1; }

}
