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
	public int guiinspector$getX() { return ((FlatButtonWidgetAccessor) this).getDim2i().x(); }
	public int guiinspector$getY() { return ((FlatButtonWidgetAccessor) this).getDim2i().y(); }
	public int guiinspector$getWidth() { return ((FlatButtonWidgetAccessor) this).getDim2i().width(); }
	public int guiinspector$getHeight() { return ((FlatButtonWidgetAccessor) this).getDim2i().height(); }
	public com.arcadesmasher.guiinspector.Dim2iAccessor guiinspector$getDim2i() { return (Dim2iAccessor) (Object) ((FlatButtonWidgetAccessor) this).getDim2i(); }
}