package com.arcadesmasher.guiinspector.data;

import com.arcadesmasher.guiinspector.GUIInspector;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;

public record DrawTextData(String display, String[] details, OrderedText text, int x, int y, int width) implements NodeData  {

    @Override
    public void drawOutline(DrawContext context) {
        GUIInspector.drawOutline(context, this.x, this.y, this.width, 9);
    }
}
