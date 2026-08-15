package com.arcadesmasher.guiinspector.mixin;

import com.arcadesmasher.guiinspector.GUIInspector;
import net.minecraft.client.MouseHandler;
//#if _1_21_11
import net.minecraft.client.input.MouseButtonInfo;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {

	//#if _1_20_1
	//$$ @Inject(method = "onPress(JIII)V", at = @At("HEAD"), cancellable = true)
	//$$ private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
	//$$ 	GUIInspector.MouseMixin_onMouseButton(this, window, button, action, ci);
	//$$ }
	//#elif _1_21_11
	@Inject(method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V", at = @At("HEAD"), cancellable = true)
	private void onMouseButton(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
		GUIInspector.MouseMixin_onMouseButton(this, window, input.button(), action, ci);
	}
	//#endif
}
