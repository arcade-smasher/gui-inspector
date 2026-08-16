package com.arcadesmasher.guiinspector.mixin;

import com.arcadesmasher.guiinspector.GUIInspector;
import com.arcadesmasher.guiinspector.data.nodedata.DrawCallData;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.swing.tree.DefaultMutableTreeNode;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.inventory.Slot;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

	@Inject(
			method = "extractGui",
			at = @At(
					value = "TAIL"//,
//					target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
//					shift = At.Shift.AFTER
			)
	)
	private void onRender(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci, @Local(name = "graphics") GuiGraphicsExtractor graphics) {
		DefaultMutableTreeNode selectedWidget = GUIInspector.widgets.getSelectedNode();
		if (selectedWidget != null) {
			Object userObject = selectedWidget.getUserObject();

			if (userObject instanceof LayoutElement widget) {
				GUIInspector.drawOutline(graphics, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
			} else if (userObject instanceof Slot slot) {
				GUIInspector.drawOutline(graphics, slot.x, slot.y, 16, 16);
			} else if (GUIInspector.sodiumLoaded && GUIInspector.sodiumCompat.hasDimensions(userObject)) {
				int[] dimensions = GUIInspector.sodiumCompat.getDimensions(userObject);
				GUIInspector.drawOutline(graphics, dimensions[0], dimensions[1], dimensions[2], dimensions[3]);
			}
		}

		DefaultMutableTreeNode selectedDrawCall = GUIInspector.drawCalls.getSelectedNode();
		if (selectedDrawCall != null) {
			Object userObj = selectedDrawCall.getUserObject();
			if (userObj instanceof DrawCallData data) data.drawOutline(graphics);
		}
	}
}
