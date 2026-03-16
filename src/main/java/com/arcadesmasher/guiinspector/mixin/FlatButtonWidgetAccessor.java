package com.arcadesmasher.guiinspector.mixin;

import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.widgets.FlatButtonWidget", remap = false)
public interface FlatButtonWidgetAccessor {
	@Accessor("drawBackground")
	boolean shouldDrawBackground();
	@Mutable
	@Accessor("drawBackground")
	void setShouldDrawBackground(boolean drawBackground);

	@Accessor("drawFrame")
	boolean shouldDrawFrame();
	@Mutable
	@Accessor("drawFrame")
	void setShouldDrawFrame(boolean drawFrame);

	@Accessor("label")
	Text getLabel();
	@Mutable
	@Accessor("label")
	void setLabel(Text label);

	@Accessor("selected")
	boolean isSelected();
	@Invoker("setSelected")
	void invokeSetSelected(boolean selected);

	@Accessor("enabled")
	boolean isEnabled();
	@Invoker("setEnabled")
	void invokeSetEnabled(boolean enabled);

	@Invoker("isVisible")
	boolean getVisible();
	@Invoker("setVisible")
	void invokeSetVisible(boolean visible);
}
