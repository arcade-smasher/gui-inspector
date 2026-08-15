package com.arcadesmasher.guiinspector.mixin;

import com.arcadesmasher.guiinspector.GUIInspector;
import com.arcadesmasher.guiinspector.data.nodedata.DrawCallData;
import com.llamalad7.mixinextras.sugar.Local;
//#if _1_21_11
import net.minecraft.client.DeltaTracker;
//#endif
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.swing.tree.DefaultMutableTreeNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.inventory.Slot;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

	//#if _1_20_1
//$$	@Inject(
//$$			method = "render",
//$$			at = @At(
//$$					value = "INVOKE",
//$$					target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V"
//$$			)
//$$	)
//$$	private void onRender(float tickDelta, long startTime, boolean tick, CallbackInfo ci, @Local GuiGraphics drawContext) {
//$$		DefaultMutableTreeNode selectedWidget = GUIInspector.widgets.getSelectedNode();
//$$		if (selectedWidget != null) {
//$$			Object userObject = selectedWidget.getUserObject();
//$$
//$$			if (userObject instanceof LayoutElement widget) {
//$$				GUIInspector.drawOutline(drawContext, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
//$$			} else if (userObject instanceof Slot slot) {
//$$				GUIInspector.drawOutline(drawContext, slot.x, slot.y, 16, 16);
//$$			} else if (GUIInspector.sodiumLoaded && GUIInspector.sodiumCompat.hasDimensions(userObject)) {
//$$				int[] dimensions = GUIInspector.sodiumCompat.getDimensions(userObject);
//$$				GUIInspector.drawOutline(drawContext, dimensions[0], dimensions[1], dimensions[2], dimensions[3]);
//$$			}
//$$		}
//$$
//$$		DefaultMutableTreeNode selectedDrawCall = GUIInspector.drawCalls.getSelectedNode();
//$$		if (selectedDrawCall != null) {
//$$			Object userObj = selectedDrawCall.getUserObject();
//$$			if (userObj instanceof DrawCallData data) data.drawOutline(drawContext);
//$$		}
//$$	}
	//#elif _1_21_11
	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"
			)
	)
	private void onRender(DeltaTracker deltaTracker, boolean tick, CallbackInfo ci, @Local GuiGraphics drawContext) {
		DefaultMutableTreeNode selectedWidget = GUIInspector.widgets.getSelectedNode();
		if (selectedWidget != null) {
			Object userObject = selectedWidget.getUserObject();

			if (userObject instanceof LayoutElement widget) {
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
	//#endif
}
