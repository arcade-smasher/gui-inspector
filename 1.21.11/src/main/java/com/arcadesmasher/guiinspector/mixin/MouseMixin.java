package com.arcadesmasher.guiinspector.mixin;

import com.arcadesmasher.guiinspector.GUIInspector;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

	@Inject(method = "onMouseButton(JLnet/minecraft/client/input/MouseInput;I)V", at = @At("HEAD"), cancellable = true)
	private void onMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
		GUIInspector.MouseMixin_onMouseButton(this, window, input.button(), action, ci);
	}

	// seems to only handle setting x and y...
//	@Inject(method = "onCursorPos", at = @At("HEAD"), cancellable = true)
//	private void onCursorPos(long window, double x, double y, CallbackInfo ci) {
//		if (GUIInspector.selectorMode && ((Mouse) (Object) this).client.currentScreen != null) {
//			ci.cancel();
//		}
//	}
}
