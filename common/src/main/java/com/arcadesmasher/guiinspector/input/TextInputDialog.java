package com.arcadesmasher.guiinspector.input;

import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.function.Consumer;
import java.util.Optional;

public class TextInputDialog extends InputDialog<Component> {

	private final ImString literalBuffer;
	private final ImString translationBuffer;
	private final ImBoolean useTranslation;

	protected TextInputDialog(String title, String label, Component initial, Consumer<Optional<Component>> callback) {
		super(title, label, initial, callback);

		literalBuffer = new ImString(initial.getString(), 256);

		String translationKey = initial.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";

		translationBuffer = new ImString(translationKey, 256);
		useTranslation = new ImBoolean(!translationKey.isEmpty());

		if (useTranslation.get()) {
			value = Component.translatable(translationKey);
		} else {
			value = Component.literal(literalBuffer.get());
		}
	}

	public static void show(String title, String label, Component initial, Consumer<Optional<Component>> callback) {
		new TextInputDialog(title, label, initial, callback).open();
	}

	public static void show(String title, String label, Consumer<Optional<Component>> callback) {
		show(title, label, Component.empty(), callback);
	}

	@Override
	protected void renderFields() {
		if (ImGui.checkbox("Use translation key instead", useTranslation)) {
			updateValue();
		}

		ImGui.setNextItemWidth(160);

		if (useTranslation.get()) {
			if (ImGui.inputText("Translation key", translationBuffer)) {
				updateValue();
			}
		} else {
			if (ImGui.inputText("Literal text", literalBuffer)) {
				updateValue();
			}
		}
	}

	private void updateValue() {
		if (useTranslation.get()) {
			value = Component.translatable(translationBuffer.get());
		} else {
			value = Component.literal(literalBuffer.get());
		}
	}
}