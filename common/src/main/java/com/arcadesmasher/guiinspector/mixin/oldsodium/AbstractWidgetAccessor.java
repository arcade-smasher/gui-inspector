package com.arcadesmasher.guiinspector.mixin.oldsodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget", remap = false)
public interface AbstractWidgetAccessor extends com.arcadesmasher.guiinspector.AbstractWidgetAccessor {
	@Invoker("isHovered")
	boolean getHovered();

	@Accessor("hovered")
	void setHovered(boolean hovered);
}
