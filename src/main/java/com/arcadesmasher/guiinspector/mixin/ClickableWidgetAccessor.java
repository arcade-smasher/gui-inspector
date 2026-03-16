package com.arcadesmasher.guiinspector.mixin;

import com.arcadesmasher.guiinspector.WidthHeightAccessor;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClickableWidget.class)
public interface ClickableWidgetAccessor extends WidthHeightAccessor {
	@Accessor("width")
	void setWidth(int width);

	@Accessor("height")
	void setHeight(int height);
}
