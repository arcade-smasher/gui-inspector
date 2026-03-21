package com.arcadesmasher.guiinspector.mixin;

import com.arcadesmasher.guiinspector.*;
import com.arcadesmasher.guiinspector.data.nodedata.*;
import com.arcadesmasher.guiinspector.mappings.ClassMappings;
import com.arcadesmasher.guiinspector.mappings.MethodMappings;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Mixin(DrawContext.class)
public class DrawContextMixin {

	@Unique
	private static List<StackWalker.StackFrame> getFrames() {
		return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.walk(s -> s.skip(2).toList());
	}

	@Unique
	private static Object[] buildGenericFrameDetails(StackWalker.StackFrame frame) {
		return buildGenericFrameDetails(frame, null);
	}

	@Unique
	private static Object[] buildGenericFrameDetails(StackWalker.StackFrame frame, String label) {
		return new Object[]{
				"Full method: " +	(label != null ? label : buildFrameLabel(frame)),
				"Class: " +			ClassMappings.getMappedName(frame.getDeclaringClass()),
				"Method: " +		MethodMappings.getMappedName(frame.getDeclaringClass(), frame.getMethodName()),
				"File: " +			frame.getFileName(),
				"Line: " +			frame.getLineNumber(),
				"Native: " +		frame.isNativeMethod(),
				"Module: " +		(frame.getDeclaringClass().getModule() != null ? frame.getDeclaringClass().getModule().getName() : "unknown"),
				"Package: " +		frame.getClassName().substring(0, Math.max(frame.getClassName().lastIndexOf('.'), 0))
		};
	}

	@Unique
	private static Object[] combineDetails(Object[] first, Object[] second) {
		Object[] combined = new Object[first.length + second.length];
		System.arraycopy(first, 0, combined, 0, first.length);
		System.arraycopy(second, 0, combined, first.length, second.length);
		return combined;
	}

	@Unique
	private static String buildFrameLabel(StackWalker.StackFrame frame) {
		StringBuilder sb = new StringBuilder();

		sb.append(ClassMappings.getMappedName(frame.getDeclaringClass()))
				.append('@')
				.append(Integer.toHexString(frame.getDeclaringClass().hashCode()))
				.append("    ")
				.append(ClassMappings.getMappedName(frame.getMethodType().returnType()))
				.append(' ')
				.append(MethodMappings.getMappedName(frame.getDeclaringClass(), frame.getMethodName()))
				.append('(');

		boolean first = true;
		for (var param : frame.getMethodType().parameterList()) {
			if (!first) sb.append(", ");
			sb.append(ClassMappings.getMappedName(param));
			first = false;
		}

		sb.append(')');

		return sb.toString();
	}

	@Unique
	private String buildSimpleFrameLabel(StackWalker.StackFrame frame) {
		return MethodMappings.getMappedName(frame.getDeclaringClass(), frame.getMethodName()) + "(...)";
	}

	// returns [a, r, g, b]
	@Unique
	private static int[] decodeColor(int color) {
		return new int[]{
				(color >> 24) & 0xFF,
				(color >> 16) & 0xFF,
				(color >> 8)  & 0xFF,
				color & 0xFF
		};
	}

	@Unique
	private static String decodeUsageFlags(int usage) {
		List<String> flags = new ArrayList<>();
		if ((usage & GpuTexture.USAGE_COPY_DST)           != 0) flags.add("COPY_DST");
		if ((usage & GpuTexture.USAGE_COPY_SRC)           != 0) flags.add("COPY_SRC");
		if ((usage & GpuTexture.USAGE_TEXTURE_BINDING)    != 0) flags.add("TEXTURE_BINDING");
		if ((usage & GpuTexture.USAGE_RENDER_ATTACHMENT)  != 0) flags.add("RENDER_ATTACHMENT");
		if ((usage & GpuTexture.USAGE_CUBEMAP_COMPATIBLE) != 0) flags.add("CUBEMAP_COMPATIBLE");
		return flags.isEmpty() ? "None" : String.join(" | ", flags);
	}


	@Inject(method = "fill(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/texture/TextureSetup;IIIIILjava/lang/Integer;)V", at = @At("HEAD"))
	private void injectFill(RenderPipeline pipeline, TextureSetup textureSetup, int x1, int y1, int x2, int y2, int color, Integer color2, CallbackInfo ci) {
		if (!GUIInspector.drawCapture) return;

		List<StackWalker.StackFrame> frames = getFrames();

		// the class that draws this mod's outline around objects. stack traces containing it are skipped because it means the stack can't have any meaningful info for the user
		boolean shouldSkip = frames.stream().anyMatch(frame -> frame.getDeclaringClass().equals(GUIInspector.class));

		if (!shouldSkip) {

			int[] argb = decodeColor(color);
			int a1 = argb[0], r1 = argb[1], g1 = argb[2], b1 = argb[3];

			String gradientInfo;
			if (color2 != null) {
				int[] argb2 = decodeColor(color2);
				gradientInfo = String.format(
						"Gradient:\n\tStart: 0x%08X RGBA(%d,%d,%d,%d)\n\tEnd:   0x%08X RGBA(%d,%d,%d,%d)",
						color, r1, g1, b1, a1,
						color2, argb2[1], argb2[2], argb2[3], argb2[0]
				);
			} else {
				gradientInfo = String.format(
						"Solid Color:\n\t0x%08X RGBA(%d,%d,%d,%d)",
						color, r1, g1, b1, a1
				);
			}

			DrawCallDataTreeBuilder[] nodes = new DrawCallDataTreeBuilder[frames.size()];

			DrawContext context = ((DrawContext) (Object) this);

			Vector2f screenPos1 = new Vector2f(x1, y1);
			Vector2f screenPos2 = new Vector2f(x2, y2);

			context.getMatrices().transformPosition(screenPos1, screenPos1);
			context.getMatrices().transformPosition(screenPos2, screenPos2);

			boolean screenDifferent = Math.round(screenPos1.x) != x1 || Math.round(screenPos1.y) != y1 || Math.round(screenPos2.x) != x2 || Math.round(screenPos2.y) != y2;

			// someone is going to hate me for this
			Object[] details = new Object[15 + (screenDifferent ? 4 : 0)];
			int j = 0;
			details[j++] = "Captured: fill(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/texture/TextureSetup;IIIIILjava/lang/Integer;)V";
			details[j++] = "";
			details[j++] = "Screen Rect:";
			details[j++] = String.format("\tTop-left: (%d, %d)", x1, y1);
			details[j++] = String.format("\tBottom-right: (%d, %d)", x2, y2);
			details[j++] = String.format("\tSize: %d x %d", x2 - x1, y2 - y1);
			if (screenDifferent) {
				details[j++] = "";
				details[j++] = String.format("\tOn-screen top-left: (%d, %d)", Math.round(screenPos1.x), Math.round(screenPos1.y));
				details[j++] = String.format("\tOn-screen bottom-right: (%d, %d)", Math.round(screenPos2.x), Math.round(screenPos2.y));
				details[j++] = String.format("\tOn-screen size: %d x %d", Math.round(screenPos2.x - screenPos1.x), Math.round(screenPos2.y - screenPos1.y));
			}
			details[j++] = "";
			details[j++] = gradientInfo;
			details[j++] = "";
			details[j++] = "Pipeline:";
			details[j++] = "\t" + ClassMappings.getMappedName(pipeline) + "@" + Integer.toHexString(pipeline.hashCode());
			details[j++] = "";
			details[j++] = "TextureSetup:";
			details[j++] = "\t" + ClassMappings.getMappedName(textureSetup) + "@" + Integer.toHexString(textureSetup.hashCode());
			details[j] = "";

			for (int i = 0; i < frames.size(); i++) {
				StackWalker.StackFrame frame = frames.get(i);
				String label = buildFrameLabel(frame);
				String simpleLabel = buildSimpleFrameLabel(frame);

				AlternatingDisplayNodeData data;

				if (i == 0) {

					data = new FillData(
							label, simpleLabel,
							combineDetails(details, buildGenericFrameDetails(frame, label)),
							Math.round(screenPos1.x), Math.round(screenPos1.y), Math.round(screenPos2.x), Math.round(screenPos2.y)
					);
				} else {
					data = new SimpleAlternatingDisplayNodeData(
							label, simpleLabel,
							buildGenericFrameDetails(frame, label)
					);
				}
				data.setAltDisplay(GUIInspector.drawCallNamesAreSimple);

				nodes[i] = new DrawCallDataTreeBuilder(label, data);
			}

			GUIInspector.treeBuilder.collectChain(nodes);
		}
	}

	@Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V", at = @At("HEAD"))
	private void injectDrawText(TextRenderer textRenderer, OrderedText text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
		if (!GUIInspector.drawCapture) return;

		List<StackWalker.StackFrame> frames = getFrames();

		boolean shouldSkip = frames.stream().anyMatch(frame -> frame.getDeclaringClass().equals(GUIInspector.class));

		if (!shouldSkip) {

			StringBuilder builder = new StringBuilder();
			text.accept((index, style, codePoint) -> {
				builder.appendCodePoint(codePoint);
				return true;
			});
			String content = builder.toString();

			int width = textRenderer.getWidth(text);
			int[] argb = decodeColor(color);
			int a = argb[0], r = argb[1], g = argb[2], b = argb[3];

			DrawCallDataTreeBuilder[] nodes = new DrawCallDataTreeBuilder[frames.size()];

			Vector2f screenPos = new Vector2f(x, y);
			((DrawContext) (Object) this).getMatrices().transformPosition(screenPos, screenPos);

			boolean screenDifferent = Math.round(screenPos.x) != x || Math.round(screenPos.y) != y;

			Object[] details = new Object[19 + (screenDifferent ? 2 : 0)];
			int j = 0;
			details[j++] = "Captured: drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V";
			details[j++] = "";
			details[j++] = "Text:";
			details[j++] = String.format("\t\"%s\"", content);
			details[j++] = "";
			details[j++] = "Position:";
			details[j++] = String.format("\t(x, y): (%d, %d)", x, y);
			details[j++] = String.format("\tWidth: %d px", width);
			if (screenDifferent) {
				details[j++] = "";
				details[j++] = String.format("\tOn-screen (x, y): (%d, %d)", Math.round(screenPos.x), Math.round(screenPos.y));
			}
			details[j++] = "";
			details[j++] = "Color:";
			details[j++] = String.format("\tHex: 0x%08X", color);
			details[j++] = String.format("\tRGBA: (%d, %d, %d, %d)", r, g, b, a);
			details[j++] = "";
			details[j++] = "Shadow:";
			details[j++] = "\t" + shadow;
			details[j++] = "";
			details[j++] = "TextRenderer:";
			details[j++] = "\t" + ClassMappings.getMappedName(textRenderer) + "@" + Integer.toHexString(textRenderer.hashCode());
			details[j] = "";

			for (int i = 0; i < frames.size(); i++) {
				StackWalker.StackFrame frame = frames.get(i);
				String label = buildFrameLabel(frame);
				String simpleLabel = buildSimpleFrameLabel(frame);

				AlternatingDisplayNodeData data;

				if (i == 0) {

					data = new DrawTextData(
							label, simpleLabel,
							combineDetails(details, buildGenericFrameDetails(frame, label)),
							Math.round(screenPos.x), Math.round(screenPos.y), width
					);
				} else {
					data = new SimpleAlternatingDisplayNodeData(
							label, simpleLabel,
							buildGenericFrameDetails(frame, label)
					);
				}
				data.setAltDisplay(GUIInspector.drawCallNamesAreSimple);

				nodes[i] = new DrawCallDataTreeBuilder(label, data);
			}

			GUIInspector.treeBuilder.collectChain(nodes);
		}
	}

	// source args seem to be named wrong. they say u1, v1, u2, v2, but it's actually u1, u2, v1, v2. same deal with drawTiledTexturedQuad
	@Inject(method = "drawTexturedQuad(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lnet/minecraft/client/gl/GpuSampler;IIIIFFFFI)V", at = @At("HEAD"))
	private void injectDrawTexturedQuad(RenderPipeline pipeline, GpuTextureView textureView, GpuSampler sampler, int x1, int y1, int x2, int y2, float u1, float u2, float v1, float v2, int color, CallbackInfo ci) {
		if (!GUIInspector.drawCapture) return;

		List<StackWalker.StackFrame> frames = getFrames();

		boolean shouldSkip = frames.stream().anyMatch(frame -> frame.getDeclaringClass().equals(GUIInspector.class));

		if (!shouldSkip) {

			GpuTexture texture = textureView.texture();

			BufferedImage buffer = null;
			if (texture instanceof GlTexture glTexture) {
				buffer = GUIInspector.getTextureSubregion(glTexture, 0, u1, v1, u2, v2);
				if (buffer == null) buffer = GUIInspector.getTextureSubregion(glTexture, 0, 0, 0, 1, 1);
			}

			int texWidth  = texture.getWidth(0);
			int texHeight = texture.getHeight(0);

			int px1 = (int)(u1 * texWidth);
			int py1 = (int)(v1 * texHeight);
			int px2 = (int)(u2 * texWidth);
			int py2 = (int)(v2 * texHeight);

			int[] argb = decodeColor(color);
			int a = argb[0], r = argb[1], g = argb[2], b = argb[3];
			float alphaPercent = (a / 255.0f) * 100f;

			String usageString = decodeUsageFlags(texture.usage());

			DrawCallDataTreeBuilder[] nodes = new DrawCallDataTreeBuilder[frames.size()];

			DrawContext context = ((DrawContext) (Object) this);

			Vector2f screenPos1 = new Vector2f(x1, y1);
			Vector2f screenPos2 = new Vector2f(x2, y2);

			context.getMatrices().transformPosition(screenPos1, screenPos1);
			context.getMatrices().transformPosition(screenPos2, screenPos2);

			boolean screenDifferent = Math.round(screenPos1.x) != x1 || Math.round(screenPos1.y) != y1 || Math.round(screenPos2.x) != x2 || Math.round(screenPos2.y) != y2;

			Object[] details = new Object[30 + (screenDifferent ? 4 : 0)];
			int j = 0;
			details[j++] = "Captured: drawTexturedQuad(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lnet/minecraft/client/gl/GpuSampler;IIIIFFFFI)V";
			details[j++] = "";
			details[j++] = "Screen Rect:";
			details[j++] = String.format("\tTop-left: (%d, %d)", x1, y1);
			details[j++] = String.format("\tBottom-right: (%d, %d)", x2, y2);
			details[j++] = String.format("\tSize: %d x %d", x2 - x1, y2 - y1);
			if (screenDifferent) {
				details[j++] = "";
				details[j++] = String.format("\tOn-screen top-left: (%d, %d)", Math.round(screenPos1.x), Math.round(screenPos1.y));
				details[j++] = String.format("\tOn-screen bottom-right: (%d, %d)", Math.round(screenPos2.x), Math.round(screenPos2.y));
				details[j++] = String.format("\tOn-screen size: %d x %d", Math.round(screenPos2.x - screenPos1.x), Math.round(screenPos2.y - screenPos1.y));
			}
			details[j++] = "";
			details[j++] = "UV Rect:";
			details[j++] = String.format("\tNormalized: (%.4f, %.4f) -> (%.4f, %.4f)", u1, v1, u2, v2);
			details[j++] = String.format("\tPixel: (%d, %d) -> (%d, %d)", px1, py1, px2, py2);
			details[j++] = "";
			details[j++] = "Color:";
			details[j++] = String.format("\tHex: 0x%08X", color);
			details[j++] = String.format("\tRGBA: (%d, %d, %d, %d)", r, g, b, a);
			details[j++] = String.format("\tAlpha: %.1f%%", alphaPercent);
			details[j++] = "";
			details[j++] = "Texture:";
			details[j++] = String.format("\tLabel: %s", texture.getLabel());
			details[j++] = String.format("\tSize: %d x %d", texWidth, texHeight);
			details[j++] = String.format("\tMip Levels: %d", texture.getMipLevels());
			details[j++] = String.format("\tLayers: %d", texture.getDepthOrLayers());
			details[j++] = String.format("\tFormat: %s", texture.getFormat());
			details[j++] = String.format("\tUsage: %s", usageString);
			details[j++] = "";
			details[j++] = "Sampler:";
			details[j++] = "\t" + ClassMappings.getMappedName(sampler) + "@" + Integer.toHexString(sampler.hashCode());
			details[j++] = "";
			details[j++] = "Image:";
			details[j++] = buffer == null ? "[Could not display image]" : buffer;
			details[j] = "";

			for (int i = 0; i < frames.size(); i++) {
				StackWalker.StackFrame frame = frames.get(i);
				String label = buildFrameLabel(frame);
				String simpleLabel = buildSimpleFrameLabel(frame);

				AlternatingDisplayNodeData data;

				if (i == 0) {

					data = new DrawTexturedQuadData(
							label, simpleLabel,
							combineDetails(details, buildGenericFrameDetails(frame, label)),
							Math.round(screenPos1.x), Math.round(screenPos1.y), Math.round(screenPos2.x), Math.round(screenPos2.y)
					);
				} else {
					data = new SimpleAlternatingDisplayNodeData(
							label, simpleLabel,
							buildGenericFrameDetails(frame, label)
					);
				}
				data.setAltDisplay(GUIInspector.drawCallNamesAreSimple);

				nodes[i] = new DrawCallDataTreeBuilder(label, data);
			}

			GUIInspector.treeBuilder.collectChain(nodes);
		}
	}

	@Inject(method = "drawTiledTexturedQuad(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lnet/minecraft/client/gl/GpuSampler;IIIIIIFFFFI)V", at = @At("HEAD"))
	private void injectDrawTiledTexturedQuad(RenderPipeline pipeline, GpuTextureView textureView, GpuSampler sampler, int tileWidth, int tileHeight, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1, int color, CallbackInfo ci) {
		if (!GUIInspector.drawCapture) return;

		List<StackWalker.StackFrame> frames = getFrames();

		boolean shouldSkip = frames.stream().anyMatch(frame -> frame.getDeclaringClass().equals(GUIInspector.class));

		if (!shouldSkip) {

			GpuTexture texture = textureView.texture();

			BufferedImage buffer = null;
			if (texture instanceof GlTexture glTexture) {
				buffer = GUIInspector.getTextureSubregion(glTexture, 0, u0, v0, u1, v1);
				if (buffer == null) buffer = GUIInspector.getTextureSubregion(glTexture, 0, 0, 0, 1, 1);
			}

			int texWidth  = texture.getWidth(0);
			int texHeight = texture.getHeight(0);

			int px0 = (int)(u0 * texWidth);
			int py0 = (int)(v0 * texHeight);
			int px1 = (int)(u1 * texWidth);
			int py1 = (int)(v1 * texHeight);

			int[] argb = decodeColor(color);
			int a = argb[0], r = argb[1], g = argb[2], b = argb[3];
			float alphaPercent = (a / 255.0f) * 100f;

			String usageString = decodeUsageFlags(texture.usage());

			DrawCallDataTreeBuilder[] nodes = new DrawCallDataTreeBuilder[frames.size()];

			DrawContext context = ((DrawContext) (Object) this);

			Vector2f screenPos0 = new Vector2f(x0, y0);
			Vector2f screenPos1 = new Vector2f(x1, y1);

			context.getMatrices().transformPosition(screenPos0, screenPos0);
			context.getMatrices().transformPosition(screenPos1, screenPos1);

			boolean screenDifferent = Math.round(screenPos0.x) != x0 || Math.round(screenPos0.y) != y0 || Math.round(screenPos1.x) != x1 || Math.round(screenPos1.y) != y1;

			Object[] details = new Object[30 + (screenDifferent ? 4 : 0)];
			int j = 0;
			details[j++] = "Captured: drawTiledTexturedQuad(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lnet/minecraft/client/gl/GpuSampler;IIIIIIFFFFI)V";
			details[j++] = "";
			details[j++] = "Screen Rect:";
			details[j++] = String.format("\tTop-left: (%d, %d)", x0, y0);
			details[j++] = String.format("\tBottom-right: (%d, %d)", x1, y1);
			details[j++] = String.format("\tSize: %d x %d", x1 - x0, y1 - y0);
			if (screenDifferent) {
				details[j++] = "";
				details[j++] = String.format("\tOn-screen top-left: (%d, %d)", Math.round(screenPos0.x), Math.round(screenPos0.y));
				details[j++] = String.format("\tOn-screen bottom-right: (%d, %d)", Math.round(screenPos1.x), Math.round(screenPos1.y));
				details[j++] = String.format("\tOn-screen size: %d x %d", Math.round(screenPos1.x - screenPos0.x), Math.round(screenPos1.y - screenPos0.y));
			}
			details[j++] = "";
			details[j++] = "UV Rect:";
			details[j++] = String.format("\tNormalized: (%.4f, %.4f) -> (%.4f, %.4f)", u0, v0, u1, v1);
			details[j++] = String.format("\tPixel: (%d, %d) -> (%d, %d)", px0, py0, px1, py1);
			details[j++] = "";
			details[j++] = "Color:";
			details[j++] = String.format("\tHex: 0x%08X", color);
			details[j++] = String.format("\tRGBA: (%d, %d, %d, %d)", r, g, b, a);
			details[j++] = String.format("\tAlpha: %.1f%%", alphaPercent);
			details[j++] = "";
			details[j++] = "Texture:";
			details[j++] = String.format("\tLabel: %s", texture.getLabel());
			details[j++] = String.format("\tSize: %d x %d", texWidth, texHeight);
			details[j++] = String.format("\tMip Levels: %d", texture.getMipLevels());
			details[j++] = String.format("\tLayers: %d", texture.getDepthOrLayers());
			details[j++] = String.format("\tFormat: %s", texture.getFormat());
			details[j++] = String.format("\tUsage: %s", usageString);
			details[j++] = "";
			details[j++] = "Sampler:";
			details[j++] = "\t" + ClassMappings.getMappedName(sampler) + "@" + Integer.toHexString(sampler.hashCode());
			details[j++] = "";
			details[j++] = "Image:";
			details[j++] = buffer == null ? "[Could not display image]" : buffer;
			details[j] = "";

			for (int i = 0; i < frames.size(); i++) {
				StackWalker.StackFrame frame = frames.get(i);
				String label = buildFrameLabel(frame);
				String simpleLabel = buildSimpleFrameLabel(frame);

				AlternatingDisplayNodeData data;

				if (i == 0) {

					data = new DrawTiledTexturedQuadData(
							label, simpleLabel,
							combineDetails(details, buildGenericFrameDetails(frame, label)),
							Math.round(screenPos0.x), Math.round(screenPos0.y), Math.round(screenPos1.x), Math.round(screenPos1.y)
					);
				} else {
					data = new SimpleAlternatingDisplayNodeData(
							label, simpleLabel,
							buildGenericFrameDetails(frame, label)
					);
				}
				data.setAltDisplay(GUIInspector.drawCallNamesAreSimple);

				nodes[i] = new DrawCallDataTreeBuilder(label, data);
			}

			GUIInspector.treeBuilder.collectChain(nodes);
		}
	}

	@Inject(method = "drawItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;III)V", at = @At("HEAD"))
	private void injectDrawItem(LivingEntity entity, World world, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
		if (!GUIInspector.drawCapture) return;

		List<StackWalker.StackFrame> frames = getFrames();

		boolean shouldSkip = frames.stream().anyMatch(frame -> frame.getDeclaringClass().equals(GUIInspector.class));

		if (!shouldSkip) {

			DrawCallDataTreeBuilder[] nodes = new DrawCallDataTreeBuilder[frames.size()];

			Vector2f screenPos = new Vector2f(x, y);
			((DrawContext) (Object) this).getMatrices().transformPosition(screenPos, screenPos);

			boolean screenDifferent = Math.round(screenPos.x) != x || Math.round(screenPos.y) != y;

			Object[] details = new Object[12 + (screenDifferent ? 1 : 0)];
			int j = 0;
			details[j++] = "Captured: drawItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;III)V";
			details[j++] = "";
			details[j++] = "Item:";
			details[j++] = String.format("\tRaw Name: %s", stack.getItem().getName());
			details[j++] = String.format("\tLiteral Name: %s", stack.getItem().getName().getString());
			details[j++] = "";
			details[j++] = "Position:";
			details[j++] = String.format("\t(x, y): (%d, %d)", x, y);
			if (screenDifferent) details[j++] = String.format("\tOn-screen (x, y): (%d, %d)", Math.round(screenPos.x), Math.round(screenPos.y));
			details[j++] = "";
			details[j++] = "Stack:";
			details[j++] = "\t" + ClassMappings.getMappedName(stack) + "@" + Integer.toHexString(stack.hashCode());
			details[j] = "";

			for (int i = 0; i < frames.size(); i++) {
				StackWalker.StackFrame frame = frames.get(i);
				String label = buildFrameLabel(frame);
				String simpleLabel = buildSimpleFrameLabel(frame);

				AlternatingDisplayNodeData data;

				if (i == 0) {

					data = new DrawItemData(
							label, simpleLabel,
							combineDetails(details, buildGenericFrameDetails(frame, label)),
							Math.round(screenPos.x), Math.round(screenPos.y)
					);
				} else {
					data = new SimpleAlternatingDisplayNodeData(
							label, simpleLabel,
							buildGenericFrameDetails(frame, label)
					);
				}
				data.setAltDisplay(GUIInspector.drawCallNamesAreSimple);

				nodes[i] = new DrawCallDataTreeBuilder(label, data);
			}

			GUIInspector.treeBuilder.collectChain(nodes);
		}
	}
}