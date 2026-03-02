package com.arcadesmasher.guiinspector.data;

import net.minecraft.client.gui.DrawContext;

public interface NodeData extends FriendlyDisplay {
    String[] details();

    void drawOutline(DrawContext context);
}
