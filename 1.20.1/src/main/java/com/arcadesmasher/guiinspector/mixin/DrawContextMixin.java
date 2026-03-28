package com.arcadesmasher.guiinspector.mixin;

import com.arcadesmasher.guiinspector.*;
import com.arcadesmasher.guiinspector.data.nodedata.*;
import com.arcadesmasher.guiinspector.mappings.ClassMappings;
import com.arcadesmasher.guiinspector.mappings.MethodMappings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.Resource;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

@Mixin(DrawContext.class)
public class DrawContextMixin {

	@Unique
	private static List<StackWalker.StackFrame> getFrames(int skip) {
		return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.walk(s -> s.skip(1 + skip).toList());
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
	private static BufferedImage getImageFromIdentifier(Identifier id) {
		try {
			MinecraftClient client = MinecraftClient.getInstance();
			Optional<Resource> resourceOptional = client.getResourceManager().getResource(id);

			if (resourceOptional.isEmpty()) {
				System.err.println("Resource not found: " + id);
				return null;
			}

			Resource resource = resourceOptional.get();

			try (InputStream stream = resource.getInputStream()) {
				return ImageIO.read(stream);
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Unique
	private static BufferedImage getImageFromIdentifier(Identifier id, float u1, float v1, float u2, float v2) {

		BufferedImage image = getImageFromIdentifier(id);

		if (image == null) return null;

		return getImageSubregion(image, u1, v1, u2, v2);
	}

	@Unique
	private static BufferedImage getImageSubregion(BufferedImage image, float u1, float v1, float u2, float v2) {

		if (image == null) return null;

		int width = image.getWidth();
		int height = image.getHeight();

		u1 = Math.max(0f, Math.min(1f, u1));
		v1 = Math.max(0f, Math.min(1f, v1));
		u2 = Math.max(0f, Math.min(1f, u2));
		v2 = Math.max(0f, Math.min(1f, v2));

		// UV to pixel coords
		int x1 = (int)(u1 * width);
		int y1 = (int)(v1 * height);
		int x2 = (int)(u2 * width);
		int y2 = (int)(v2 * height);

		// handle flipped UVs
		int minX = Math.min(x1, x2);
		int minY = Math.min(y1, y2);
		int maxX = Math.max(x1, x2);
		int maxY = Math.max(y1, y2);

		int subWidth = maxX - minX;
		int subHeight = maxY - minY;

		if (subWidth <= 0 || subHeight <= 0) {
			System.err.println("Invalid UV region for: " + image);
			return null;
		}

		// Clamp to image bounds
		subWidth = Math.min(subWidth, width - minX);
		subHeight = Math.min(subHeight, height - minY);

		return image.getSubimage(minX, minY, subWidth, subHeight);
	}

	@Inject(method = "fill(Lnet/minecraft/client/render/RenderLayer;IIIIII)V", at = @At("HEAD"))
	private void injectFill(RenderLayer layer, int x1, int y1, int x2, int y2, int z, int color, CallbackInfo ci) {
		if (!GUIInspector.drawCapture) return;

		List<StackWalker.StackFrame> frames = getFrames(1);

		// the class that draws this mod's outline around objects. stack traces containing it are skipped because it means the stack can't have any meaningful info for the user
		boolean shouldSkip = frames.stream().anyMatch(frame -> frame.getDeclaringClass().equals(GUIInspector.class));

		if (!shouldSkip) {

			int[] argb = decodeColor(color);
			int a1 = argb[0], r1 = argb[1], g1 = argb[2], b1 = argb[3];

			DrawCallDataTreeBuilder[] nodes = new DrawCallDataTreeBuilder[frames.size()];

			DrawContext context = ((DrawContext) (Object) this);

			Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

			Vector3f screenPos1 = new Vector3f(x1, y1, 0.0f);
			Vector3f screenPos2 = new Vector3f(x2, y2, 0.0f);

			screenPos1.mulPosition(matrix);
			screenPos2.mulPosition(matrix);

			boolean screenDifferent = Math.round(screenPos1.x) != x1 || Math.round(screenPos1.y) != y1 || Math.round(screenPos2.x) != x2 || Math.round(screenPos2.y) != y2;

			// someone is going to hate me for this
			Object[] details = new Object[13 + (screenDifferent ? 4 : 0)];
			int j = 0;
			details[j++] = "Captured: fill(Lnet/minecraft/client/render/RenderLayer;IIIIII)V";
			details[j++] = "";
			details[j++] = "Screen Rect:";
			details[j++] = String.format("\tTop-left: (%d, %d)", x1, y1);
			details[j++] = String.format("\tBottom-right: (%d, %d)", x2, y2);
			details[j++] = String.format("\tSize: %d x %d", x2 - x1, y2 - y1);
			details[j++] = String.format("\tDepth (z): %d", z);
			if (screenDifferent) {
				details[j++] = "";
				details[j++] = String.format("\tOn-screen top-left: (%d, %d)", Math.round(screenPos1.x), Math.round(screenPos1.y));
				details[j++] = String.format("\tOn-screen bottom-right: (%d, %d)", Math.round(screenPos2.x), Math.round(screenPos2.y));
				details[j++] = String.format("\tOn-screen size: %d x %d", Math.round(screenPos2.x - screenPos1.x), Math.round(screenPos2.y - screenPos1.y));
			}
			details[j++] = "";
			details[j++] = String.format("Solid Color:\n\t0x%08X RGBA(%d,%d,%d,%d)", color, r1, g1, b1, a1);
			details[j++] = "";
			details[j++] = "RenderLayer:";
			details[j++] = "\t" + ClassMappings.getMappedName(layer) + "@" + Integer.toHexString(layer.hashCode());
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

	@Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I", at = @At("HEAD"))
	private void injectDrawText1(TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
		injectDrawText(textRenderer, () -> text, () -> textRenderer.getWidth(text), x, y, color, shadow);
	}

	@Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)I", at = @At("HEAD"))
	private void injectDrawText2(TextRenderer textRenderer, OrderedText text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
		injectDrawText(textRenderer, () -> {
			StringBuilder builder = new StringBuilder();
			text.accept((index, style, codePoint) -> {
				builder.appendCodePoint(codePoint);
				return true;
			});
			return builder.toString();
		}, () -> textRenderer.getWidth(text),x, y, color, shadow);
	}

	@Unique
	private void injectDrawText(TextRenderer textRenderer, Supplier<String> getText, IntSupplier getWidth, int x, int y, int color, boolean shadow) {
		if (!GUIInspector.drawCapture) return;

		List<StackWalker.StackFrame> frames = getFrames(2);

		boolean shouldSkip = frames.stream().anyMatch(frame -> frame.getDeclaringClass().equals(GUIInspector.class));

		if (!shouldSkip) {

			String content = getText.get();

			int width = getWidth.getAsInt();
			int[] argb = decodeColor(color);
			int a = argb[0], r = argb[1], g = argb[2], b = argb[3];

			DrawCallDataTreeBuilder[] nodes = new DrawCallDataTreeBuilder[frames.size()];

			Matrix4f matrix = ((DrawContext) (Object) this).getMatrices().peek().getPositionMatrix();
			Vector3f screenPos = new Vector3f(x, y, 0.0f);
			screenPos.mulPosition(matrix);

			boolean screenDifferent = Math.round(screenPos.x) != x || Math.round(screenPos.y) != y;

			Object[] details = new Object[19 + (screenDifferent ? 2 : 0)];
			int j = 0;
			details[j++] = "Captured: drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)I";
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
	@Inject(method = "drawTexturedQuad(Lnet/minecraft/util/Identifier;IIIIIFFFF)V", at = @At("HEAD"))
	private void injectDrawTexturedQuad1(Identifier texture, int x1, int x2, int y1, int y2, int z, float u1, float u2, float v1, float v2, CallbackInfo ci) {
		if (!GUIInspector.drawCapture) return;

		List<StackWalker.StackFrame> frames = getFrames(1);

		boolean shouldSkip = frames.stream().anyMatch(frame -> frame.getDeclaringClass().equals(GUIInspector.class));

		if (!shouldSkip) {

			BufferedImage image = getImageFromIdentifier(texture);

			BufferedImage subregion = getImageSubregion(image, u1, v1, u2, v2);

			int texWidth;
			int texHeight;

			if (subregion == null) {
				texWidth = 0;
				texHeight = 0;
			} else {
				texWidth  = image.getWidth();
				texHeight = image.getHeight();
			}

			int px1 = (int)(u1 * texWidth);
			int py1 = (int)(v1 * texHeight);
			int px2 = (int)(u2 * texWidth);
			int py2 = (int)(v2 * texHeight);

			DrawCallDataTreeBuilder[] nodes = new DrawCallDataTreeBuilder[frames.size()];

			DrawContext context = ((DrawContext) (Object) this);

			Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

			Vector3f screenPos1 = new Vector3f(x1, y1, 0.0f);
			Vector3f screenPos2 = new Vector3f(x2, y2, 0.0f);

			screenPos1.mulPosition(matrix);
			screenPos2.mulPosition(matrix);

			boolean screenDifferent = Math.round(screenPos1.x) != x1 || Math.round(screenPos1.y) != y1 || Math.round(screenPos2.x) != x2 || Math.round(screenPos2.y) != y2;

			Object[] details = new Object[15 + (screenDifferent ? 4 : 0)];
			int j = 0;
			details[j++] = "Captured: drawTexturedQuad(Lnet/minecraft/util/Identifier;IIIIIFFFF)V";
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
			details[j++] = "Image:";
			details[j++] = String.format("\tSize: %d x %d", texWidth, texHeight);
			details[j++] = subregion == null ? "[Could not display image]" : subregion;
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


	@Inject(method = "drawTexturedQuad(Lnet/minecraft/util/Identifier;IIIIIFFFFFFFF)V", at = @At("HEAD"))
	private void injectDrawTexturedQuad2(Identifier texture, int x1, int x2, int y1, int y2, int z, float u1, float u2, float v1, float v2, float red, float green, float blue, float alpha, CallbackInfo ci) {
		if (!GUIInspector.drawCapture) return;

		List<StackWalker.StackFrame> frames = getFrames(1);

		boolean shouldSkip = frames.stream().anyMatch(frame -> frame.getDeclaringClass().equals(GUIInspector.class));

		if (!shouldSkip) {

			BufferedImage image = getImageFromIdentifier(texture);

			BufferedImage subregion = getImageSubregion(image, u1, v1, u2, v2);

			int texWidth;
			int texHeight;

			if (subregion == null) {
				texWidth = 0;
				texHeight = 0;
			} else {
				texWidth  = image.getWidth();
				texHeight = image.getHeight();
			}

			int px1 = (int)(u1 * texWidth);
			int py1 = (int)(v1 * texHeight);
			int px2 = (int)(u2 * texWidth);
			int py2 = (int)(v2 * texHeight);

			DrawCallDataTreeBuilder[] nodes = new DrawCallDataTreeBuilder[frames.size()];

			DrawContext context = ((DrawContext) (Object) this);

			Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

			Vector3f screenPos1 = new Vector3f(x1, y1, 0.0f);
			Vector3f screenPos2 = new Vector3f(x2, y2, 0.0f);

			screenPos1.mulPosition(matrix);
			screenPos2.mulPosition(matrix);

			boolean screenDifferent = Math.round(screenPos1.x) != x1 || Math.round(screenPos1.y) != y1 || Math.round(screenPos2.x) != x2 || Math.round(screenPos2.y) != y2;

			Object[] details = new Object[20 + (screenDifferent ? 4 : 0)];
			int j = 0;
			details[j++] = "Captured: drawTexturedQuad(Lnet/minecraft/util/Identifier;IIIIIFFFFFFFF)V";
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
			details[j++] = String.format("\tHex: 0x%08X", (Math.round(alpha) & 0xFF) << 24 | (Math.round(red) & 0xFF) << 16 | (Math.round(green) & 0xFF) << 8 | (Math.round(blue) & 0xFF));
			details[j++] = String.format("\tRGBA: (%d, %d, %d, %d)", (int) (red * 255), (int) (green * 255), (int) (blue * 255), (int) (alpha * 255));
			details[j++] = String.format("\tAlpha: %.1f%%", alpha * 100);
			details[j++] = "";
			details[j++] = "Image:";
			details[j++] = String.format("\tSize: %d x %d", texWidth, texHeight);
			details[j++] = subregion == null ? "[Could not display image]" : subregion;
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

	@Inject(method = "drawItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;IIII)V", at = @At("HEAD"))
	private void injectDrawItem(LivingEntity entity, World world, ItemStack stack, int x, int y, int seed, int z, CallbackInfo ci) {
		if (!GUIInspector.drawCapture) return;

		List<StackWalker.StackFrame> frames = getFrames(1);

		boolean shouldSkip = frames.stream().anyMatch(frame -> frame.getDeclaringClass().equals(GUIInspector.class));

		if (!shouldSkip) {

			DrawCallDataTreeBuilder[] nodes = new DrawCallDataTreeBuilder[frames.size()];

			Vector2f screenPos = new Vector2f(x, y);
			Vector3f pos = new Vector3f(x, y, 0.0f);
			Matrix4f matrix = ((DrawContext) (Object) this).getMatrices().peek().getPositionMatrix();
			pos.mulPosition(matrix);

			boolean screenDifferent = Math.round(screenPos.x) != x || Math.round(screenPos.y) != y;

			Object[] details = new Object[13 + (screenDifferent ? 1 : 0)];
			int j = 0;
			details[j++] = "Captured: drawItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;III)V";
			details[j++] = "";
			details[j++] = "Item:";
			details[j++] = String.format("\tRaw Name: %s", stack.getItem().getName());
			details[j++] = String.format("\tLiteral Name: %s", stack.getItem().getName().getString());
			details[j++] = "";
			details[j++] = "Position:";
			details[j++] = String.format("\tDepth (z): %d", z);
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