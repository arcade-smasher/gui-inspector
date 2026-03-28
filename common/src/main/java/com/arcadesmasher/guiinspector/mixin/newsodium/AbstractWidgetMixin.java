package com.arcadesmasher.guiinspector.mixin.newsodium;

import com.arcadesmasher.guiinspector.DimensionedMirror;
import net.caffeinemc.mods.sodium.client.gui.Dimensioned;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget", remap = false)
@Implements(@Interface(iface = DimensionedMirror.class, prefix = "guiinspector$"))
public abstract class AbstractWidgetMixin implements DimensionedMirror {
	public int guiinspector$getX() { return ((Dimensioned) this).getDimensions().x(); }
	public int guiinspector$getY() { return ((Dimensioned) this).getDimensions().y(); }
	public int guiinspector$getWidth() { return ((Dimensioned) this).getDimensions().width(); }
	public int guiinspector$getHeight() { return ((Dimensioned) this).getDimensions().height(); }
	public com.arcadesmasher.guiinspector.Dim2iAccessor guiinspector$getDim2i() { return (Dim2iAccessor) (Object) ((Dimensioned) this).getDimensions(); }
}