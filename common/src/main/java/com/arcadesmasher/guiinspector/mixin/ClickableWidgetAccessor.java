package com.arcadesmasher.guiinspector.mixin;

import com.arcadesmasher.guiinspector.WidthHeightAccessor;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractWidget.class)
public interface ClickableWidgetAccessor extends WidthHeightAccessor {
	@Accessor("width")
	void setWidth(int width);

	@Accessor("height")
	void setHeight(int height);

	@Accessor("alpha")
	float acquireAlpha();
}
