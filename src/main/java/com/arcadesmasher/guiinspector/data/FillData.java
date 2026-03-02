package com.arcadesmasher.guiinspector.data;

import com.arcadesmasher.guiinspector.GUIInspector;
import net.minecraft.client.gui.DrawContext;

public record FillData(String display, String[] details, int x1, int y1, int x2, int y2) implements NodeData {

    @Override
    public void drawOutline(DrawContext context) {
        GUIInspector.drawOutline(context, this.x1, this.y1, this.x2 - this.x1, this.y2 - this.y1);
    }
}
