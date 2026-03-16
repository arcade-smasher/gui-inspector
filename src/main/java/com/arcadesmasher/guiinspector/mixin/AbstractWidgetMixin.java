package com.arcadesmasher.guiinspector.mixin;

import com.arcadesmasher.guiinspector.DimensionedMirror;
import net.caffeinemc.mods.sodium.client.gui.Dimensioned;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget", remap = false)
@Implements(@Interface(iface = DimensionedMirror.class, prefix = "guiinspector$"))
public abstract class AbstractWidgetMixin implements DimensionedMirror {
	public int guiinspector$getX() { return ((Dimensioned) (Object) this).getDimensions().x(); }
	public int guiinspector$getY() { return ((Dimensioned) (Object) this).getDimensions().y(); }
	public int guiinspector$getWidth() { return ((Dimensioned) (Object) this).getDimensions().width(); }
	public int guiinspector$getHeight() { return ((Dimensioned) (Object) this).getDimensions().height(); }
	public Dim2i guiinspector$getDim2i() { return ((Dimensioned) (Object) this).getDimensions(); }
}