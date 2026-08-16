package com.arcadesmasher.guiinspector.mixin.sodium;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.widgets.CenteredFlatWidget", remap = false)
public interface CenteredFlatWidgetAccessor {
	@Accessor("isSelectable")
	boolean isSelectable();
	@Mutable
	@Accessor("isSelectable")
	void setIsSelectable(boolean isSelectable);

	@Accessor("selected")
	boolean isSelected();
	@Invoker("setSelected")
	void invokeSetSelected(boolean selected);

	@Accessor("enabled")
	boolean isEnabled();
	@Invoker("setEnabled")
	void invokeSetEnabled(boolean enabled);

	@Accessor("visible")
	boolean isVisible();
	@Invoker("setVisible")
	void invokeSetVisible(boolean visible);

	@Accessor("label")
	Component getLabel();
	@Mutable
	@Accessor("label")
	void setLabel(Component label);

	@Accessor("subtitle")
	Component getSubtitle();
	@Mutable
	@Accessor("subtitle")
	void setSubtitle(Component subtitle);
}
