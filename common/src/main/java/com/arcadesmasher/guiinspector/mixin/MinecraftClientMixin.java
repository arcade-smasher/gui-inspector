package com.arcadesmasher.guiinspector.mixin;

import com.arcadesmasher.guiinspector.GUIInspector;
import com.arcadesmasher.guiinspector.data.nodedata.AbstractNodeData;
import com.arcadesmasher.guiinspector.data.nodedata.TextureSnapshot;
import com.arcadesmasher.guiinspector.tree.TreeBuilder;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL11.glDeleteTextures;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

	@Inject(method = "renderFrame", at = @At("HEAD"))
	private void injectRenderHead(boolean advanceGameTime, CallbackInfo ci) {
		GUIInspector.totalFramesRendered++;
		if (GUIInspector.pendingCapture) {
			GUIInspector.pendingCapture = false;
			GUIInspector.drawCapture = true;

			GUIInspector.treeBuilder = new TreeBuilder(); // not cleared at end of render in order to still access the chains list from it. cleared at next capture instead (right here)
		}
	}

	@Inject(method = "renderFrame", at = @At("RETURN"))
	private void injectRenderReturn(boolean advanceGameTime, CallbackInfo ci) {
		if (GUIInspector.drawCapture) {
			if (GUIInspector.drawCallViewIsTree) {
				GUIInspector.treeBuilder.buildTree(GUIInspector.drawCalls);
			} else {
				GUIInspector.treeBuilder.buildFlatTree(GUIInspector.drawCalls);
			}

			GUIInspector.drawCalls.collapseAll();
			GUIInspector.drawCalls.getSideMenuEntries().forEach(entry -> {
				if (entry instanceof TextureSnapshot textureSnapshot && textureSnapshot.glId() > 0) {
					glDeleteTextures(textureSnapshot.glId());
				}
			});
			GUIInspector.drawCalls.clearSideMenu();

			Minecraft mc = Minecraft.getInstance();
			GUIInspector.drawCalls.getRootNode().setUserObject(new AbstractNodeData("Capture", new Object[] {
					"Frame: " + GUIInspector.totalFramesRendered,
					"",
					//#if _26_1
					"Active screen: " + (mc.screen == null ? "None" : GUIInspector.getMappedClassName(mc.screen)),
					//#elif _26_2
//$$					"Active screen: " + (mc.gui.screen() == null ? "None" : GUIInspector.getMappedClassName(mc.gui.screen())),
					//#endif
					"",
					"Dimensions:",
					"\tSize (width, height): " + mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight(),
					"\tScaled size (width, height): " + mc.getWindow().getGuiScaledWidth() + "x" + mc.getWindow().getGuiScaledHeight(),
					"",
					"Draw calls: " + GUIInspector.totalDrawCalls,
					"Draw calls (fill): " + GUIInspector.totalFillDrawCalls,
					"Draw calls (text): " + GUIInspector.totalTextDrawCalls,
					"Draw calls (blit): " + GUIInspector.totalTexturedQuadDrawCalls,
					"Draw calls (tiledBlit): " + GUIInspector.totalTiledTexturedQuadDrawCalls,
					"Draw calls (item): " + GUIInspector.totalItemDrawCalls
			}) {});

			GUIInspector.drawCalls.setSelectedNode(GUIInspector.drawCalls.getRootNode());
			GUIInspector.drawCalls.expandNode(GUIInspector.drawCalls.getRootNode());
		}

		GUIInspector.drawCapture = false;
		GUIInspector.totalDrawCalls = 0;
		GUIInspector.totalFillDrawCalls = 0;
		GUIInspector.totalTextDrawCalls = 0;
		GUIInspector.totalTexturedQuadDrawCalls = 0;
		GUIInspector.totalTiledTexturedQuadDrawCalls = 0;
		GUIInspector.totalItemDrawCalls = 0;
	}
}