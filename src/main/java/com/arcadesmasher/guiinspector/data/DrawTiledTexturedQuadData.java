package com.arcadesmasher.guiinspector.data;

import com.arcadesmasher.guiinspector.GUIInspector;
import net.minecraft.client.gui.DrawContext;

public record DrawTiledTexturedQuadData(String display, String[] details, int x0, int y0, int x1, int y1) implements NodeData {

    @Override
    public void drawOutline(DrawContext context) {
        GUIInspector.drawOutline(context, this.x0, this.y0, this.x1 - this.x0, this.y1 - this.y0);
    }
}
