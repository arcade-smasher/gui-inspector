package com.arcadesmasher.guiinspector.mixin.oldsodium;

import com.arcadesmasher.guiinspector.DimensionedMirror;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget", remap = false)
@Implements(@Interface(iface = DimensionedMirror.class, prefix = "guiinspector$"))
public abstract class FlatButtonWidgetMixin implements DimensionedMirror {
	public int guiinspector$getX() { return ((com.arcadesmasher.guiinspector.mixin.oldsodium.FlatButtonWidgetAccessor) this).getDim2i().x(); }
	public int guiinspector$getY() { return ((com.arcadesmasher.guiinspector.mixin.oldsodium.FlatButtonWidgetAccessor) this).getDim2i().y(); }
	public int guiinspector$getWidth() { return ((com.arcadesmasher.guiinspector.mixin.oldsodium.FlatButtonWidgetAccessor) this).getDim2i().width(); }
	public int guiinspector$getHeight() { return ((com.arcadesmasher.guiinspector.mixin.oldsodium.FlatButtonWidgetAccessor) this).getDim2i().height(); }
	public com.arcadesmasher.guiinspector.Dim2iAccessor guiinspector$getDim2i() { return (com.arcadesmasher.guiinspector.mixin.oldsodium.Dim2iAccessor) (Object) ((com.arcadesmasher.guiinspector.mixin.oldsodium.FlatButtonWidgetAccessor) this).getDim2i(); }
}