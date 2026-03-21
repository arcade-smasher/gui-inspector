package com.arcadesmasher.guiinspector.mixin;

import com.arcadesmasher.guiinspector.GUIInspector;
import com.arcadesmasher.guiinspector.SodiumCompat;
import com.arcadesmasher.guiinspector.data.nodedata.DrawCallData;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.swing.tree.DefaultMutableTreeNode;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"
			)
	)
	private void onRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci, @Local DrawContext drawContext) {
		DefaultMutableTreeNode selectedWidget = GUIInspector.widgets.getSelectedNode();
		if (selectedWidget != null) {
			Object userObject = selectedWidget.getUserObject();

			if (userObject instanceof Widget widget) {
				GUIInspector.drawOutline(drawContext, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
			} else if (userObject instanceof Slot slot) {
				GUIInspector.drawOutline(drawContext, slot.x, slot.y, 16, 16);
			} else if (GUIInspector.sodiumLoaded && SodiumCompat.isDimensioned(userObject)) {
				int[] dimensions = SodiumCompat.getDimensions(userObject);
				GUIInspector.drawOutline(drawContext, dimensions[0], dimensions[1], dimensions[2], dimensions[3]);
			}
		}

		DefaultMutableTreeNode selectedDrawCall = GUIInspector.drawCalls.getSelectedNode();
		if (selectedDrawCall != null) {
			Object userObj = selectedDrawCall.getUserObject();
			if (userObj instanceof DrawCallData data) data.drawOutline(drawContext);
		}
	}
}
