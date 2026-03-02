package com.arcadesmasher.guiinspector.mixin;

import com.arcadesmasher.guiinspector.*;
import com.arcadesmasher.guiinspector.data.DrawTextData;
import com.arcadesmasher.guiinspector.data.DrawTexturedQuadData;
import com.arcadesmasher.guiinspector.data.DrawTiledTexturedQuadData;
import com.arcadesmasher.guiinspector.data.FillData;
import com.arcadesmasher.guiinspector.mappings.ClassMappings;
import com.arcadesmasher.guiinspector.mappings.MethodMappings;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(DrawContext.class)
public class DrawContextMixin {

    @Inject(method = "fill(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/texture/TextureSetup;IIIIILjava/lang/Integer;)V", at = @At("HEAD"))
    private void injectFill(RenderPipeline pipeline, TextureSetup textureSetup, int x1, int y1, int x2, int y2, int color, Integer color2, CallbackInfo ci) {
        if (!GUIInspector.drawCapture) return;

        List<StackWalker.StackFrame> frames =
                StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(s -> s.skip(1).toList());

        DefaultMutableTreeNode currentParent = GUIInspector.drawCalls.getRootNode();

        int width = x2 - x1;
        int height = y2 - y1;

        int a1 = (color >> 24) & 0xFF;
        int r1 = (color >> 16) & 0xFF;
        int g1 = (color >> 8) & 0xFF;
        int b1 = color & 0xFF;

        String gradientInfo;

        if (color2 != null) {
            int c2 = color2;
            int a2 = (c2 >> 24) & 0xFF;
            int r2 = (c2 >> 16) & 0xFF;
            int g2 = (c2 >> 8) & 0xFF;
            int b2 = c2 & 0xFF;

            gradientInfo = String.format(
                    "Gradient:\n\tStart: 0x%08X RGBA(%d,%d,%d,%d)\n\tEnd:   0x%08X RGBA(%d,%d,%d,%d)",
                    color, r1, g1, b1, a1,
                    c2, r2, g2, b2, a2
            );
        } else {
            gradientInfo = String.format(
                    "Solid Color:\n\t0x%08X RGBA(%d,%d,%d,%d)",
                    color, r1, g1, b1, a1
            );
        }

        String[] nodeDetails = {
                "Origin: fill(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/texture/TextureSetup;IIIIILjava/lang/Integer;)V",
                "",
                "Screen Rect:",
                String.format("\tTop-left: (%d, %d)", x1, y1),
                String.format("\tBottom-right: (%d, %d)", x2, y2),
                String.format("\tSize: %d x %d", width, height),
                "",
                gradientInfo,
                "",
                "Pipeline:",
                "\t" + ClassMappings.getMappedName(pipeline) + "@" + Integer.toHexString(pipeline.hashCode()),
                "",
                "TextureSetup:",
                "\t" + ClassMappings.getMappedName(textureSetup) + "@" + Integer.toHexString(textureSetup.hashCode())
        };

        for (StackWalker.StackFrame frame : frames) {
            currentParent = GUIInspector.drawCalls.addChildNode(currentParent, new FillData(
                    ClassMappings.getMappedName(frame.getDeclaringClass()) + "@" + Integer.toHexString(frame.getDeclaringClass().hashCode()) + "    " + ClassMappings.getMappedName(frame.getMethodType().returnType()) + " " + MethodMappings.getMappedName(frame.getClassName() + "." + frame.getMethodName()) + "(" + frame.getMethodType().parameterList().stream().map(ClassMappings::getMappedName).collect(Collectors.joining(", ")) + ")",
                    nodeDetails,
                    x1, y1, x2, y2
            ));
        }
    }

    @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V", at = @At("HEAD"))
    private void injectDrawText(TextRenderer textRenderer, OrderedText text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        if (!GUIInspector.drawCapture) return;

        List<StackWalker.StackFrame> frames =
                StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(s -> s.skip(1).toList());

        DefaultMutableTreeNode currentParent = GUIInspector.drawCalls.getRootNode();

        // convert OrderedText to String safely
        StringBuilder builder = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            builder.appendCodePoint(codePoint);
            return true;
        });
        String content = builder.toString();

        int width = textRenderer.getWidth(text);

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        String[] nodeDetails = new String[]{
                "Origin: drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V",
                "",
                "Text:",
                String.format("\t\"%s\"", content),
                "",
                "Position:",
                String.format("\t(x, y): (%d, %d)", x, y),
                String.format("\tWidth: %d px", width),
                "",
                "Color:",
                String.format("\tHex: 0x%08X", color),
                String.format("\tRGBA: (%d, %d, %d, %d)", r, g, b, a),
                "",
                "Shadow:",
                "\t" + shadow,
                "",
                "TextRenderer:",
                "\t" + ClassMappings.getMappedName(textRenderer) + "@" + Integer.toHexString(textRenderer.hashCode())
        };

        for (StackWalker.StackFrame frame : frames) {
            currentParent = GUIInspector.drawCalls.addChildNode(currentParent, new DrawTextData(
                    ClassMappings.getMappedName(frame.getDeclaringClass()) + "@" + Integer.toHexString(frame.getDeclaringClass().hashCode()) + "    " + ClassMappings.getMappedName(frame.getMethodType().returnType()) + " " + MethodMappings.getMappedName(frame.getClassName() + "." + frame.getMethodName()) + "(" + frame.getMethodType().parameterList().stream().map(ClassMappings::getMappedName).collect(Collectors.joining(", ")) + ")",
                    nodeDetails,
                    text, x, y, width
            ));
        }
    }

    @Inject(method = "drawTexturedQuad(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lnet/minecraft/client/gl/GpuSampler;IIIIFFFFI)V", at = @At("HEAD"))
    private void injectDrawTexturedQuad(RenderPipeline pipeline, GpuTextureView textureView, GpuSampler sampler, int x1, int y1, int x2, int y2, float u1, float v1, float u2, float v2, int color, CallbackInfo ci) {
        if (!GUIInspector.drawCapture) return;

        List<StackWalker.StackFrame> frames =
                StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(s -> s.skip(1).toList());

        DefaultMutableTreeNode currentParent = GUIInspector.drawCalls.getRootNode();

        GpuTexture texture = textureView.texture();

        int texWidth = texture.getWidth(0);
        int texHeight = texture.getHeight(0);

        // convert UV to pixel space. doesn't work correctly, think i need to divide or something
        int px1 = (int)(u1 * texWidth);
        int py1 = (int)(v1 * texHeight);
        int px2 = (int)(u2 * texWidth);
        int py2 = (int)(v2 * texHeight);

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        float alphaPercent = (a / 255.0f) * 100f;

        // decode usage flags
        int usage = texture.usage();
        List<String> usageFlags = new ArrayList<>();

        if ((usage & GpuTexture.USAGE_COPY_DST) != 0) usageFlags.add("COPY_DST");
        if ((usage & GpuTexture.USAGE_COPY_SRC) != 0) usageFlags.add("COPY_SRC");
        if ((usage & GpuTexture.USAGE_TEXTURE_BINDING) != 0) usageFlags.add("TEXTURE_BINDING");
        if ((usage & GpuTexture.USAGE_RENDER_ATTACHMENT) != 0) usageFlags.add("RENDER_ATTACHMENT");
        if ((usage & GpuTexture.USAGE_CUBEMAP_COMPATIBLE) != 0) usageFlags.add("CUBEMAP_COMPATIBLE");

        String usageString = usageFlags.isEmpty() ? "None" : String.join(" | ", usageFlags);

        String[] nodeDetails = new String[]{
                "Origin: drawTexturedQuad(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lnet/minecraft/client/gl/GpuSampler;IIIIFFFFI)V",
                "",
                "Screen Rect:",
                String.format("\tTop-left: (%d, %d)", x1, y1),
                String.format("\tBottom-right: (%d, %d)", x2, y2),
                String.format("\tSize: %d x %d", x2 - x1, y2 - y1),
                "",
                "UV Rect:",
                String.format("\tNormalized: (%.4f, %.4f) -> (%.4f, %.4f)", u1, v1, u2, v2),
                String.format("\tPixel: (%d, %d) -> (%d, %d)", px1, py1, px2, py2),
                "",
                "Color:",
                String.format("\tHex: 0x%08X", color),
                String.format("\tRGBA: (%d, %d, %d, %d)", r, g, b, a),
                String.format("\tAlpha: %.1f%%", alphaPercent),
                "",
                "Texture:",
                String.format("\tLabel: %s", texture.getLabel()),
                String.format("\tSize: %d x %d", texWidth, texHeight),
                String.format("\tMip Levels: %d", texture.getMipLevels()),
                String.format("\tLayers: %d", texture.getDepthOrLayers()),
                String.format("\tFormat: %s", texture.getFormat()),
                String.format("\tUsage: %s", usageString),
                "",
                "Sampler:",
                "\t" + ClassMappings.getMappedName(sampler) + "@" + Integer.toHexString(sampler.hashCode())
        };

        for (StackWalker.StackFrame frame : frames) {
            currentParent = GUIInspector.drawCalls.addChildNode(currentParent, new DrawTexturedQuadData(
                    ClassMappings.getMappedName(frame.getDeclaringClass()) + "@" + Integer.toHexString(frame.getDeclaringClass().hashCode()) + "    " + ClassMappings.getMappedName(frame.getMethodType().returnType()) + " " + MethodMappings.getMappedName(frame.getClassName() + "." + frame.getMethodName()) + "(" + frame.getMethodType().parameterList().stream().map(ClassMappings::getMappedName).collect(Collectors.joining(", ")) + ")",
                    nodeDetails,
                    x1, y1, x2, y2
                ));
        }
    }



    @Inject(method = "drawTiledTexturedQuad(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lnet/minecraft/client/gl/GpuSampler;IIIIIIFFFFI)V", at = @At("HEAD"))
    private void injectDrawTiledTexturedQuad(RenderPipeline pipeline, GpuTextureView textureView, GpuSampler sampler, int tileWidth, int tileHeight, int x0, int y0, int x1, int y1, float u0, float v0, float u1, float v1, int color, CallbackInfo ci) {
        if (!GUIInspector.drawCapture) return;

        List<StackWalker.StackFrame> frames =
                StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(s -> s.skip(1).toList());

        DefaultMutableTreeNode currentParent = GUIInspector.drawCalls.getRootNode();

        GpuTexture texture = textureView.texture();

        int texWidth = texture.getWidth(0);
        int texHeight = texture.getHeight(0);

        int px0 = (int)(u0 * texWidth);
        int py0 = (int)(v0 * texHeight);
        int px1 = (int)(u1 * texWidth);
        int py1 = (int)(v1 * texHeight);

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        float alphaPercent = (a / 255.0f) * 100f;

        // decode usage flags
        int usage = texture.usage();
        List<String> usageFlags = new ArrayList<>();

        if ((usage & GpuTexture.USAGE_COPY_DST) != 0) usageFlags.add("COPY_DST");
        if ((usage & GpuTexture.USAGE_COPY_SRC) != 0) usageFlags.add("COPY_SRC");
        if ((usage & GpuTexture.USAGE_TEXTURE_BINDING) != 0) usageFlags.add("TEXTURE_BINDING");
        if ((usage & GpuTexture.USAGE_RENDER_ATTACHMENT) != 0) usageFlags.add("RENDER_ATTACHMENT");
        if ((usage & GpuTexture.USAGE_CUBEMAP_COMPATIBLE) != 0) usageFlags.add("CUBEMAP_COMPATIBLE");

        String usageString = usageFlags.isEmpty() ? "None" : String.join(" | ", usageFlags);

        String[] nodeDetails = new String[]{
                "Origin: drawTexturedQuad(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lnet/minecraft/client/gl/GpuSampler;IIIIFFFFI)V",
                "",
                "Screen Rect:",
                String.format("\tTop-left: (%d, %d)", x0, y0),
                String.format("\tBottom-right: (%d, %d)", x1, y1),
                String.format("\tSize: %d x %d", x1 - x0, y1 - y0),
                "",
                "UV Rect:",
                String.format("\tNormalized: (%.4f, %.4f) -> (%.4f, %.4f)", u0, v0, u1, v1),
                String.format("\tPixel: (%d, %d) -> (%d, %d)", px0, py0, px1, py1),
                "",
                "Color:",
                String.format("\tHex: 0x%08X", color),
                String.format("\tRGBA: (%d, %d, %d, %d)", r, g, b, a),
                String.format("\tAlpha: %.1f%%", alphaPercent),
                "",
                "Texture:",
                String.format("\tLabel: %s", texture.getLabel()),
                String.format("\tSize: %d x %d", texWidth, texHeight),
                String.format("\tMip Levels: %d", texture.getMipLevels()),
                String.format("\tLayers: %d", texture.getDepthOrLayers()),
                String.format("\tFormat: %s", texture.getFormat()),
                String.format("\tUsage: %s", usageString),
                "",
                "Sampler:",
                "\t" + ClassMappings.getMappedName(sampler) + "@" + Integer.toHexString(sampler.hashCode())
        };

        for (StackWalker.StackFrame frame : frames) {
            currentParent = GUIInspector.drawCalls.addChildNode(currentParent, new DrawTiledTexturedQuadData(
                    ClassMappings.getMappedName(frame.getDeclaringClass()) + "@" + Integer.toHexString(frame.getDeclaringClass().hashCode()) + "    " + ClassMappings.getMappedName(frame.getMethodType().returnType()) + " " + MethodMappings.getMappedName(frame.getClassName() + "." + frame.getMethodName()) + "(" + frame.getMethodType().parameterList().stream().map(ClassMappings::getMappedName).collect(Collectors.joining(", ")) + ")",
                    nodeDetails,
                    x0, y0, x1, y1
            ));
        }
    }
}