package com.arcadesmasher.guiinspector.mixin;

import com.arcadesmasher.guiinspector.GUIInspector;
import com.arcadesmasher.guiinspector.TreeBuilder;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

	@Inject(method = "render", at = @At("HEAD"))
	private void injectRenderHead(boolean tick, CallbackInfo ci) {
		if (GUIInspector.pendingCapture) {
			GUIInspector.pendingCapture = false;
			GUIInspector.drawCapture = true;

			GUIInspector.treeBuilder = new TreeBuilder(); // not cleared at end of render in order to still access the chains list from it. cleared at next capture instead (right here)
		}
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void injectRenderReturn(boolean tick, CallbackInfo ci) {
		if (GUIInspector.drawCapture) {
			if (GUIInspector.drawCallViewIsTree) GUIInspector.treeBuilder.buildTree(GUIInspector.drawCalls);
			else GUIInspector.treeBuilder.buildFlatTree(GUIInspector.drawCalls);

			javax.swing.SwingUtilities.invokeLater(() -> {
				for (int i = GUIInspector.drawCalls.getTree().getRowCount() - 1; i > 0; i--) {
					GUIInspector.drawCalls.getTree().collapseRow(i);
				}
			});
		}
		GUIInspector.drawCapture = false;
	}
}