package com.arcadesmasher.guiinspector.mixin;

import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.util.Dim2i", remap = false)
public interface Dim2iAccessor {
	@Accessor("x")
	int getX();

	@Accessor("y")
	int getY();

	@Accessor("width")
	int getWidth();

	@Accessor("height")
	int getHeight();

	@Mutable
	@Accessor("x")
	void setX(int x);

	@Mutable
	@Accessor("y")
	void setY(int y);

	@Mutable
	@Accessor("width")
	void setWidth(int width);

	@Mutable
	@Accessor("height")
	void setHeight(int height);
}
