package com.arcadesmasher.guiinspector.data.nodedata;

import com.arcadesmasher.guiinspector.GUIInspector;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;

public final class DrawItemData extends AlternatingDisplayNodeData implements DrawCallData {
	private final int x;
	private final int y;

	public DrawItemData(String display, String altDisplay, Object[] details, int x, int y) {
		super(display, altDisplay, details);
		this.x = x;
		this.y = y;
	}

	@Override
	public void drawOutline(DrawContext context) {
		GUIInspector.drawOutline(context, this.x, this.y, 16, 16);
	}

	public int x() { return x; }

	public int y() { return y; }

}
