package com.arcadesmasher.guiinspector.data.nodedata;

import com.arcadesmasher.guiinspector.GUIInspector;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;

public final class DrawTextData extends AlternatingDisplayNodeData implements DrawCallData {
	private final OrderedText text;
	private final int x;
	private final int y;
	private final int width;

	public DrawTextData(String display, String altDisplay, Object[] details, OrderedText text, int x, int y, int width) {
		super(display, altDisplay, details);
		this.text = text;
		this.x = x;
		this.y = y;
		this.width = width;
	}

	@Override
	public void drawOutline(DrawContext context) {
		GUIInspector.drawOutline(context, this.x, this.y, this.width, 9);
	}

	public OrderedText text() { return text; }

	public int x() { return x; }

	public int y() { return y; }

	public int width() { return width; }

}
