package com.arcadesmasher.guiinspector.mixin;

import com.arcadesmasher.guiinspector.GUIInspector;
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
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void injectRenderReturn(boolean tick, CallbackInfo ci) {
        if (GUIInspector.drawCapture) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                for (int i = GUIInspector.drawCalls.getTree().getRowCount() - 1; i > 0; i--) {
                    GUIInspector.drawCalls.getTree().collapseRow(i);
                }
            });
        }
        GUIInspector.drawCapture = false;
    }
}