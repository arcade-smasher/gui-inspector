package com.arcadesmasher.guiinspector.mixin;

import com.arcadesmasher.guiinspector.GUIInspector;
import com.arcadesmasher.guiinspector.data.nodedata.DrawCallData;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.swing.tree.DefaultMutableTreeNode;

@Mixin(GameRenderer.class)
public class GameRendererMixin {


	@Dynamic("Exists in 1.20.1")
	// Lnet/minecraft/client/render/GameRenderer;render(FJZ)V
	@Inject(
			method = "Lnet/minecraft/class_757;method_3192(FJZ)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/DrawContext;draw()V"
			),
			remap = false,
			require = 0
	)
	private void onRender1_20_1(float tickDelta, long startTime, boolean tick, CallbackInfo ci, @Local DrawContext drawContext) {
		DefaultMutableTreeNode selectedWidget = GUIInspector.widgets.getSelectedNode();
		if (selectedWidget != null) {
			Object userObject = selectedWidget.getUserObject();

			if (userObject instanceof Widget widget) {
				GUIInspector.drawOutline(drawContext, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
			} else if (userObject instanceof Slot slot) {
				GUIInspector.drawOutline(drawContext, slot.x, slot.y, 16, 16);
			} else if (GUIInspector.sodiumLoaded && GUIInspector.sodiumCompat.hasDimensions(userObject)) {
				int[] dimensions = GUIInspector.sodiumCompat.getDimensions(userObject);
				GUIInspector.drawOutline(drawContext, dimensions[0], dimensions[1], dimensions[2], dimensions[3]);
			}
		}

		DefaultMutableTreeNode selectedDrawCall = GUIInspector.drawCalls.getSelectedNode();
		if (selectedDrawCall != null) {
			Object userObj = selectedDrawCall.getUserObject();
			if (userObj instanceof DrawCallData data) data.drawOutline(drawContext);
		}
	}

	@Dynamic("Exists in 1.21.11")
	// Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V
	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/class_11228;method_70890(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"
			),
			remap = false,
			require = 0
	)
	private void onRender1_21_11(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci, @Local DrawContext drawContext) {
		DefaultMutableTreeNode selectedWidget = GUIInspector.widgets.getSelectedNode();
		if (selectedWidget != null) {
			Object userObject = selectedWidget.getUserObject();

			if (userObject instanceof Widget widget) {
				GUIInspector.drawOutline(drawContext, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
			} else if (userObject instanceof Slot slot) {
				GUIInspector.drawOutline(drawContext, slot.x, slot.y, 16, 16);
			} else if (GUIInspector.sodiumLoaded && GUIInspector.sodiumCompat.hasDimensions(userObject)) {
				int[] dimensions = GUIInspector.sodiumCompat.getDimensions(userObject);
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
