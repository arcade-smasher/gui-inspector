package com.arcadesmasher.guiinspector.mixin;

import com.arcadesmasher.guiinspector.*;
import com.arcadesmasher.guiinspector.data.nodedata.*;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.joml.Vector2f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.ARBCopyImage;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;

//#if _26_2
//$$	import com.mojang.blaze3d.buffers.GpuBuffer;
//$$	import com.mojang.blaze3d.vulkan.*;
//$$	import org.lwjgl.opengl.GL12C;
//$$	import org.lwjgl.system.MemoryUtil;
//#endif

@Mixin(GuiGraphicsExtractor.class)
public class DrawContextMixin {

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

	@Inject(method = "innerFill", at = @At("HEAD"))
	private void injectFill(RenderPipeline pipeline, TextureSetup textureSetup, int x1, int y1, int x2, int y2, int color, Integer color2, CallbackInfo ci) {
		GUIInspector.totalDrawCalls++;
		GUIInspector.totalFillDrawCalls++;
		if (!GUIInspector.drawCapture) return;

		List<StackWalker.StackFrame> frames = GUIInspector.getFrames(1);

		boolean shouldSkip = frames.stream().anyMatch(frame -> frame.getDeclaringClass().equals(GUIInspector.class));

		if (!shouldSkip) {

			int[] argb = GUIInspector.decodeColor(color);
			int a1 = argb[0], r1 = argb[1], g1 = argb[2], b1 = argb[3];

			String gradientInfo;
			if (color2 != null) {
				int[] argb2 = GUIInspector.decodeColor(color2);
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

			GuiGraphicsExtractor context = ((GuiGraphicsExtractor) (Object) this);

			Vector2f screenPos1 = new Vector2f(x1, y1);
			Vector2f screenPos2 = new Vector2f(x2, y2);

			context.pose().transformPosition(screenPos1, screenPos1);
			context.pose().transformPosition(screenPos2, screenPos2);

			boolean screenDifferent = Math.round(screenPos1.x) != x1 || Math.round(screenPos1.y) != y1 || Math.round(screenPos2.x) != x2 || Math.round(screenPos2.y) != y2;

			Supplier<Object[]> computeDetails = () -> {
				Object[] details = new Object[15 + (screenDifferent ? 4 : 0)];
				int j = 0;
				details[j++] = "Captured: " + Descriptors.getDescriptorFromFrame(frames.get(0));
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
				details[j++] = GUIInspector.getMappedClassName(RenderPipeline.class) + ":";
				details[j++] = "\t" + GUIInspector.getMappedClassName(pipeline) + "@" + Integer.toHexString(pipeline.hashCode());
				details[j++] = "";
				details[j++] = GUIInspector.getMappedClassName(TextureSetup.class) + ":";
				details[j++] = "\t" + GUIInspector.getMappedClassName(textureSetup) + "@" + Integer.toHexString(textureSetup.hashCode());
				details[j] = "";
				return details;
			};

			for (int i = 0; i < frames.size(); i++) {
				StackWalker.StackFrame frame = frames.get(i);

				String label = GUIInspector.buildFrameLabel(frame);
				String simpleLabel = GUIInspector.buildSimpleFrameLabel(frame);

				AlternatingDisplayNodeData data;

				if (i == 0) {

					Object[] details = GUIInspector.combineDetails(computeDetails.get(), GUIInspector.buildGenericFrameDetails(frame, label));

					data = new FillData(
							label, simpleLabel,
							details,
							Math.round(screenPos1.x), Math.round(screenPos1.y), Math.round(screenPos2.x), Math.round(screenPos2.y)
					);
				} else {

					Object[] details = GUIInspector.buildGenericFrameDetails(frame, label);

					data = new SimpleAlternatingDisplayNodeData(
							label, simpleLabel,
							details
					);
				}
				data.setAltDisplay(!GUIInspector.useDetailedDrawCallNames);

				nodes[i] = new DrawCallDataTreeBuilder(GUIInspector.buildFrameLabelNoMap(frame), data);
			}

			GUIInspector.treeBuilder.collectChain(nodes);
		}
	}

	@Inject(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V", at = @At("HEAD"))
	private void injectDrawText(Font textRenderer, FormattedCharSequence text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
		GUIInspector.totalDrawCalls++;
		GUIInspector.totalTextDrawCalls++;
		if (!GUIInspector.drawCapture) return;

		List<StackWalker.StackFrame> frames = GUIInspector.getFrames(1);

		boolean shouldSkip = frames.stream().anyMatch(frame -> frame.getDeclaringClass().equals(GUIInspector.class));

		if (!shouldSkip) {

			StringBuilder builder = new StringBuilder();
			text.accept((index, style, codePoint) -> {
				builder.appendCodePoint(codePoint);
				return true;
			});
			String content = builder.toString();

			int width = textRenderer.width(text);
			int[] argb = GUIInspector.decodeColor(color);
			int a = argb[0], r = argb[1], g = argb[2], b = argb[3];

			DrawCallDataTreeBuilder[] nodes = new DrawCallDataTreeBuilder[frames.size()];

			Vector2f screenPos = new Vector2f(x, y);
			((GuiGraphicsExtractor) (Object) this).pose().transformPosition(screenPos, screenPos);

			boolean screenDifferent = Math.round(screenPos.x) != x || Math.round(screenPos.y) != y;

			Supplier<Object[]> computeDetails = () -> {
				Object[] details = new Object[19 + (screenDifferent ? 2 : 0)];
				int j = 0;
				details[j++] = "Captured: " + Descriptors.getDescriptorFromFrame(frames.get(0));
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
				details[j++] = GUIInspector.getMappedClassName(Font.class) + ":";
				details[j++] = "\t" + GUIInspector.getMappedClassName(textRenderer) + "@" + Integer.toHexString(textRenderer.hashCode());
				details[j] = "";
				return details;
			};

			for (int i = 0; i < frames.size(); i++) {
				StackWalker.StackFrame frame = frames.get(i);

				String label = GUIInspector.buildFrameLabel(frame);
				String simpleLabel = GUIInspector.buildSimpleFrameLabel(frame);

				AlternatingDisplayNodeData data;

				if (i == 0) {

					Object[] details = GUIInspector.combineDetails(computeDetails.get(), GUIInspector.buildGenericFrameDetails(frame, label));

					data = new DrawTextData(
							label, simpleLabel,
							details,
							Math.round(screenPos.x), Math.round(screenPos.y), width
					);
				} else {

					Object[] details = GUIInspector.buildGenericFrameDetails(frame, label);

					data = new SimpleAlternatingDisplayNodeData(
							label, simpleLabel,
							details
					);
				}
				data.setAltDisplay(!GUIInspector.useDetailedDrawCallNames);

				nodes[i] = new DrawCallDataTreeBuilder(GUIInspector.buildFrameLabelNoMap(frame), data);
			}

			GUIInspector.treeBuilder.collectChain(nodes);
		}
	}

	// source args seem to be named wrong. they say u1, v1, u2, v2, but it's actually u1, u2, v1, v2. same deal with drawTiledTexturedQuad
	@Inject(method = "innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;IIIIFFFFI)V", at = @At("HEAD"))
	private void injectDrawTexturedQuad(RenderPipeline pipeline, GpuTextureView textureView, GpuSampler sampler, int x1, int y1, int x2, int y2, float u1, float u2, float v1, float v2, int color, CallbackInfo ci) {
		GUIInspector.totalDrawCalls++;
		GUIInspector.totalTexturedQuadDrawCalls++;
		if (!GUIInspector.drawCapture) return;

		List<StackWalker.StackFrame> frames = GUIInspector.getFrames(1);

		boolean shouldSkip = frames.stream().anyMatch(frame -> frame.getDeclaringClass().equals(GUIInspector.class));

		if (!shouldSkip) {

			GpuTexture texture = textureView.texture();

			TextureSnapshot textureCopy = getTextureSnapshot(texture, 0, u1, v1, u2, v2);

			int texWidth  = texture.getWidth(0);
			int texHeight = texture.getHeight(0);

			int px1 = (int)(u1 * texWidth);
			int py1 = (int)(v1 * texHeight);
			int px2 = (int)(u2 * texWidth);
			int py2 = (int)(v2 * texHeight);

			int[] argb = GUIInspector.decodeColor(color);
			int a = argb[0], r = argb[1], g = argb[2], b = argb[3];
			float alphaPercent = (a / 255.0f) * 100f;

			String usageString = decodeUsageFlags(texture.usage());

			DrawCallDataTreeBuilder[] nodes = new DrawCallDataTreeBuilder[frames.size()];

			GuiGraphicsExtractor context = ((GuiGraphicsExtractor) (Object) this);

			Vector2f screenPos1 = new Vector2f(x1, y1);
			Vector2f screenPos2 = new Vector2f(x2, y2);

			context.pose().transformPosition(screenPos1, screenPos1);
			context.pose().transformPosition(screenPos2, screenPos2);

			boolean screenDifferent = Math.round(screenPos1.x) != x1 || Math.round(screenPos1.y) != y1 || Math.round(screenPos2.x) != x2 || Math.round(screenPos2.y) != y2;

			Supplier<Object[]> computeDetails = () -> {
				Object[] details = new Object[30 + (screenDifferent ? 4 : 0)];
				int j = 0;
				details[j++] = "Captured: " + Descriptors.getDescriptorFromFrame(frames.get(0));
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
				details[j++] = GUIInspector.getMappedClassName(GpuSampler.class) + ":";
				details[j++] = "\t" + GUIInspector.getMappedClassName(sampler) + "@" + Integer.toHexString(sampler.hashCode());
				details[j++] = "";
				details[j++] = "Image:";
				details[j++] = textureCopy != null ? textureCopy : TextureSnapshot.EMPTY;
				details[j] = "";
				return details;
			};

			for (int i = 0; i < frames.size(); i++) {
				StackWalker.StackFrame frame = frames.get(i);

				String label = GUIInspector.buildFrameLabel(frame);
				String simpleLabel = GUIInspector.buildSimpleFrameLabel(frame);

				AlternatingDisplayNodeData data;

				if (i == 0) {

					Object[] details = GUIInspector.combineDetails(computeDetails.get(), GUIInspector.buildGenericFrameDetails(frame, label));

					data = new DrawTexturedQuadData(
							label, simpleLabel,
							details,
							Math.round(screenPos1.x), Math.round(screenPos1.y), Math.round(screenPos2.x), Math.round(screenPos2.y)
					);
				} else {

					Object[] details = GUIInspector.buildGenericFrameDetails(frame, label);

					data = new SimpleAlternatingDisplayNodeData(
							label, simpleLabel,
							details
					);
				}
				data.setAltDisplay(!GUIInspector.useDetailedDrawCallNames);

				nodes[i] = new DrawCallDataTreeBuilder(GUIInspector.buildFrameLabelNoMap(frame), data);
			}

			GUIInspector.treeBuilder.collectChain(nodes);
		}
	}

	@Inject(method = "innerTiledBlit", at = @At("HEAD"))
	private void injectDrawTiledTexturedQuad(RenderPipeline pipeline, GpuTextureView textureView, GpuSampler sampler, int tileWidth, int tileHeight, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1, int color, CallbackInfo ci) {
		GUIInspector.totalDrawCalls++;
		GUIInspector.totalTiledTexturedQuadDrawCalls++;
		if (!GUIInspector.drawCapture) return;

		List<StackWalker.StackFrame> frames = GUIInspector.getFrames(1);

		boolean shouldSkip = frames.stream().anyMatch(frame -> frame.getDeclaringClass().equals(GUIInspector.class));

		if (!shouldSkip) {

			GpuTexture texture = textureView.texture();

			TextureSnapshot textureCopy = getTextureSnapshot(texture, 0, u0, v0, u1, v1);

			int texWidth  = texture.getWidth(0);
			int texHeight = texture.getHeight(0);

			int px0 = (int)(u0 * texWidth);
			int py0 = (int)(v0 * texHeight);
			int px1 = (int)(u1 * texWidth);
			int py1 = (int)(v1 * texHeight);

			int[] argb = GUIInspector.decodeColor(color);
			int a = argb[0], r = argb[1], g = argb[2], b = argb[3];
			float alphaPercent = (a / 255.0f) * 100f;

			String usageString = decodeUsageFlags(texture.usage());

			DrawCallDataTreeBuilder[] nodes = new DrawCallDataTreeBuilder[frames.size()];

			GuiGraphicsExtractor context = ((GuiGraphicsExtractor) (Object) this);

			Vector2f screenPos0 = new Vector2f(x0, y0);
			Vector2f screenPos1 = new Vector2f(x1, y1);

			context.pose().transformPosition(screenPos0, screenPos0);
			context.pose().transformPosition(screenPos1, screenPos1);

			boolean screenDifferent = Math.round(screenPos0.x) != x0 || Math.round(screenPos0.y) != y0 || Math.round(screenPos1.x) != x1 || Math.round(screenPos1.y) != y1;

			Supplier<Object[]> computeDetails = () -> {
				Object[] details = new Object[30 + (screenDifferent ? 4 : 0)];
				int j = 0;
				details[j++] = "Captured: " + Descriptors.getDescriptorFromFrame(frames.get(0));
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
				details[j++] = GUIInspector.getMappedClassName(GpuSampler.class) + ":";
				details[j++] = "\t" + GUIInspector.getMappedClassName(sampler) + "@" + Integer.toHexString(sampler.hashCode());
				details[j++] = "";
				details[j++] = "Image:";
				details[j++] = textureCopy != null ? textureCopy : TextureSnapshot.EMPTY;
				details[j] = "";
				return details;
			};

			for (int i = 0; i < frames.size(); i++) {
				StackWalker.StackFrame frame = frames.get(i);

				String label = GUIInspector.buildFrameLabel(frame);
				String simpleLabel = GUIInspector.buildSimpleFrameLabel(frame);

				AlternatingDisplayNodeData data;

				if (i == 0) {

					Object[] details = GUIInspector.combineDetails(computeDetails.get(), GUIInspector.buildGenericFrameDetails(frame, label));

					data = new DrawTiledTexturedQuadData(
							label, simpleLabel,
							details,
							Math.round(screenPos0.x), Math.round(screenPos0.y), Math.round(screenPos1.x), Math.round(screenPos1.y)
					);
				} else {

					Object[] details = GUIInspector.buildGenericFrameDetails(frame, label);

					data = new SimpleAlternatingDisplayNodeData(
							label, simpleLabel,
							details
					);
				}
				data.setAltDisplay(!GUIInspector.useDetailedDrawCallNames);

				nodes[i] = new DrawCallDataTreeBuilder(GUIInspector.buildFrameLabelNoMap(frame), data);
			}

			GUIInspector.treeBuilder.collectChain(nodes);
		}
	}

	@Inject(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"))
	private void injectDrawItem(LivingEntity entity, Level world, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
		GUIInspector.totalDrawCalls++;
		GUIInspector.totalItemDrawCalls++;
		if (!GUIInspector.drawCapture) return;

		List<StackWalker.StackFrame> frames = GUIInspector.getFrames(1);

		boolean shouldSkip = frames.stream().anyMatch(frame -> frame.getDeclaringClass().equals(GUIInspector.class));

		if (!shouldSkip) {

			DrawCallDataTreeBuilder[] nodes = new DrawCallDataTreeBuilder[frames.size()];

			Vector2f screenPos = new Vector2f(x, y);
			((GuiGraphicsExtractor) (Object) this).pose().transformPosition(screenPos, screenPos);

			boolean screenDifferent = Math.round(screenPos.x) != x || Math.round(screenPos.y) != y;

			Supplier<Object[]> computeDetails = () -> {
				Object[] details = new Object[12 + (screenDifferent ? 1 : 0)];
				int j = 0;
				details[j++] = "Captured: " + Descriptors.getDescriptorFromFrame(frames.get(0));
				details[j++] = "";
				details[j++] = "Item:";
				details[j++] = String.format("\tRaw Name: %s", stack.getItem().getName(stack));
				details[j++] = String.format("\tLiteral Name: %s", stack.getItem().getName(stack).getString());
				details[j++] = "";
				details[j++] = "Position:";
				details[j++] = String.format("\t(x, y): (%d, %d)", x, y);
				if (screenDifferent)
					details[j++] = String.format("\tOn-screen (x, y): (%d, %d)", Math.round(screenPos.x), Math.round(screenPos.y));
				details[j++] = "";
				details[j++] = "Stack:";
				details[j++] = "\t" + GUIInspector.getMappedClassName(stack) + "@" + Integer.toHexString(stack.hashCode());
				details[j] = "";
				return details;
			};

			for (int i = 0; i < frames.size(); i++) {
				StackWalker.StackFrame frame = frames.get(i);

				String label = GUIInspector.buildFrameLabel(frame);
				String simpleLabel = GUIInspector.buildSimpleFrameLabel(frame);

				AlternatingDisplayNodeData data;

				if (i == 0) {

					Object[] details = GUIInspector.combineDetails(computeDetails.get(), GUIInspector.buildGenericFrameDetails(frame, label));

					data = new DrawItemData(
							label, simpleLabel,
							details,
							Math.round(screenPos.x), Math.round(screenPos.y)
					);
				} else {

					Object[] details = GUIInspector.buildGenericFrameDetails(frame, label);

					data = new SimpleAlternatingDisplayNodeData(
							label, simpleLabel,
							details
					);
				}
				data.setAltDisplay(!GUIInspector.useDetailedDrawCallNames);

				nodes[i] = new DrawCallDataTreeBuilder(GUIInspector.buildFrameLabelNoMap(frame), data);
			}

			GUIInspector.treeBuilder.collectChain(nodes);
		}
	}

	@Unique
	private TextureSnapshot getTextureSnapshot(GpuTexture texture, int mipLevel, float u1, float v1, float u2, float v2) {
		TextureSnapshot textureCopy = null;
		if (texture instanceof GlTexture glTexture) {
			textureCopy = getTextureSubregion(glTexture, 0, u1, v1, u2, v2);
			if (textureCopy == null) textureCopy = getTextureSubregion(glTexture, 0, 0, 0, 1, 1);
		}
		//#if _26_2
//$$		else if (texture instanceof VulkanGpuTexture vulkanTexture) {
//$$			textureCopy = getTextureSubregion(vulkanTexture, 0, u1, v1, u2, v2);
//$$			if (textureCopy == null) textureCopy = getTextureSubregion(vulkanTexture, 0, 0, 0, 1, 1);
//$$		}
		//#endif
		return textureCopy;
	}

	// make sure to clean up resulting glId
	// returns a glId for the custom gl context; will only work in that context
	@Unique
	private TextureSnapshot getTextureSubregion(GlTexture texture, int mipLevel, float u1, float v1, float u2, float v2) {

		int texWidth = texture.getWidth(mipLevel);
		int texHeight = texture.getHeight(mipLevel);
		int sourceId = texture.glId();

		float uMin = Math.min(u1, u2);
		float uMax = Math.max(u1, u2);
		float vMin = Math.min(v1, v2);
		float vMax = Math.max(v1, v2);

		int xStart = Math.clamp((int) Math.floor(uMin * texWidth), 0, texWidth);
		int xEnd   = Math.clamp((int) Math.ceil(uMax * texWidth), 0, texWidth);
		int yStart = Math.clamp((int) Math.floor(vMin * texHeight), 0, texHeight);
		int yEnd   = Math.clamp((int) Math.ceil(vMax * texHeight), 0, texHeight);

		int width = xEnd - xStart;
		int height = yEnd - yStart;

		if (width <= 0 || height <= 0) {
			return null;
		}

		ByteBuffer pixels = MemoryUtil.memAlloc(width * height * 4);

		try {
			glBindTexture(GL_TEXTURE_2D, sourceId);

			ByteBuffer fullImage =
					MemoryUtil.memAlloc(texWidth * texHeight * 4);

			try {
				glGetTexImage(
						GL_TEXTURE_2D,
						mipLevel,
						GL_RGBA,
						GL_UNSIGNED_BYTE,
						fullImage
				);

				int sourceRowBytes = texWidth * 4;
				int destinationRowBytes = width * 4;

				for (int row = 0; row < height; row++) {
					int sourceOffset =
							(yStart + row) * sourceRowBytes + xStart * 4;

					int destinationOffset =
							row * destinationRowBytes;

					MemoryUtil.memCopy(
							MemoryUtil.memAddress(fullImage) + sourceOffset,
							MemoryUtil.memAddress(pixels) + destinationOffset,
							destinationRowBytes
					);
				}
			} finally {
				MemoryUtil.memFree(fullImage);
			}

			glBindTexture(GL_TEXTURE_2D, 0);


			int destinationId = GUIInspector.treeWindow.getGlWindow().callInContext(() -> {

				int id = glGenTextures();

				glBindTexture(GL_TEXTURE_2D, id);

				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE);

				glTexImage2D(
						GL_TEXTURE_2D,
						0,
						GL_RGBA8,
						width,
						height,
						0,
						GL_RGBA,
						GL_UNSIGNED_BYTE,
						pixels
				);

				glBindTexture(GL_TEXTURE_2D, 0);

				return id;
			});

			return new TextureSnapshot(destinationId, width, height);

		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			MemoryUtil.memFree(pixels);
		}
	}

	//#if _26_2
//$$	@Unique
//$$	private TextureSnapshot getTextureSubregion(VulkanGpuTexture texture, int mipLevel, float u1, float v1, float u2, float v2) {
//$$
//$$		int texWidth = texture.getWidth(mipLevel);
//$$		int texHeight = texture.getHeight(mipLevel);
//$$
//$$		float uMin = Math.min(u1, u2);
//$$		float uMax = Math.max(u1, u2);
//$$		float vMin = Math.min(v1, v2);
//$$		float vMax = Math.max(v1, v2);
//$$
//$$		int xStart = Math.clamp((int) Math.floor(uMin * texWidth), 0, texWidth);
//$$		int xEnd   = Math.clamp((int) Math.ceil(uMax * texWidth), 0, texWidth);
//$$		int yStart = Math.clamp((int) Math.floor(vMin * texHeight), 0, texHeight);
//$$		int yEnd   = Math.clamp((int) Math.ceil(vMax * texHeight), 0, texHeight);
//$$
//$$		int width = xEnd - xStart;
//$$		int height = yEnd - yStart;
//$$
//$$		if (width <= 0 || height <= 0) {
//$$			return null;
//$$		}
//$$
//$$		VulkanDevice device = (VulkanDevice) (((VulkanGpuTextureAccessor) texture).getDevice());
//$$		VulkanCommandEncoder encoder = device.createCommandEncoder();
//$$
//$$		long bufferSize = (long) width * height * 4;
//$$
//$$		int readbackUsage = GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_MAP_WRITE;
//$$		VulkanGpuBuffer readbackBuffer = device.createBuffer(() -> "TextureSubregionReadback", readbackUsage, bufferSize);
//$$
//$$		ByteBuffer pixels = MemoryUtil.memAlloc((int) bufferSize);
//$$
//$$		try {
//$$			encoder.copyTextureToBuffer(texture, readbackBuffer, 0, () -> {}, mipLevel, xStart, yStart, width, height);
//$$
//$$			encoder.submit();
//$$			var fence = encoder.createFence();
//$$			if (!fence.awaitCompletion(5000)) {
//$$				throw new IllegalStateException("Timed out waiting for texture readback");
//$$			}
//$$
//$$			var mapped = readbackBuffer.map(0, bufferSize, true, false);
//$$			try {
//$$				MemoryUtil.memCopy(MemoryUtil.memAddress(mapped.data()), MemoryUtil.memAddress(pixels), bufferSize);
//$$			} finally {
//$$				mapped.close();
//$$			}
//$$
//$$			int destinationId = GUIInspector.treeWindow.getGlWindow().callInContext(() -> {
//$$				int id = glGenTextures();
//$$
//$$				glBindTexture(GL_TEXTURE_2D, id);
//$$
//$$				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
//$$				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
//$$				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE);
//$$				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE);
//$$
//$$				glTexImage2D(
//$$						GL_TEXTURE_2D,
//$$						0,
//$$						GL_RGBA8,
//$$						width,
//$$						height,
//$$						0,
//$$						GL_RGBA,
//$$						GL_UNSIGNED_BYTE,
//$$						pixels
//$$				);
//$$
//$$				glBindTexture(GL_TEXTURE_2D, 0);
//$$
//$$				return id;
//$$			});
//$$
//$$			return new TextureSnapshot(destinationId, width, height);
//$$
//$$		} catch (Exception e) {
//$$			throw new RuntimeException(e);
//$$		} finally {
//$$			MemoryUtil.memFree(pixels);
//$$			readbackBuffer.close();
//$$		}
//$$	}
	//#endif
}