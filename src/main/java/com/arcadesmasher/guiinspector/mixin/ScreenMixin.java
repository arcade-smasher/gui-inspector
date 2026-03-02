package com.arcadesmasher.guiinspector.mixin;

import com.arcadesmasher.guiinspector.*;
import com.arcadesmasher.guiinspector.data.NodeData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.swing.tree.DefaultMutableTreeNode;

@Mixin(Screen.class)
public class ScreenMixin {

	@Inject(method = "render", at = @At("TAIL"))
	private void injectRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {

		Screen screen = (Screen)(Object)this;

		DefaultMutableTreeNode selectedWidget = GUIInspector.widgets.getSelectedNode();
		if (selectedWidget != null && selectedWidget.getUserObject() instanceof Widget widget) {
			GUIInspector.drawOutline(context, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
		}

		DefaultMutableTreeNode selectedDrawCall = GUIInspector.drawCalls.getSelectedNode();
		if (selectedDrawCall != null) {
			Object userObj = selectedDrawCall.getUserObject();
			if (userObj instanceof NodeData data) data.drawOutline(context);
		}

		if (!GUIInspector.selectorMode) return;

		for (Widget widget : GUIInspector.findAllDeepestWidgetsAt(screen.children(), mouseX, mouseY)) {
			GUIInspector.drawOutline(context, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
		}
	}
}