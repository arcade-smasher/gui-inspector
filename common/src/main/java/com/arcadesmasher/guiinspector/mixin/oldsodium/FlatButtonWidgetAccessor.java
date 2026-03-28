package com.arcadesmasher.guiinspector.mixin.oldsodium;

import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget", remap = false)
public interface FlatButtonWidgetAccessor extends com.arcadesmasher.guiinspector.FlatButtonWidgetAccessor {

	@Invoker("getLabel")
	Text acquireLabel();
	@Invoker("setLabel")
	void changeLabel(Text label);

	@Accessor("selected")
	boolean isSelected();
	@Invoker("setSelected")
	void invokeSetSelected(boolean selected);

	@Accessor("enabled")
	boolean isEnabled();
	@Invoker("setEnabled")
	void invokeSetEnabled(boolean enabled);

	@Accessor("visible")
	boolean getVisible();
	@Invoker("setVisible")
	void invokeSetVisible(boolean visible);

	@Accessor("dim")
	Dim2i getDim2i();
}
