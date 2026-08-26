package com.arcadesmasher.guiinspector.input;

import imgui.type.ImFloat;

import java.util.Optional;
import java.util.function.Consumer;

public class FloatInputDialog extends InputDialog<Float> {

	private final ImFloat buffer;

	protected FloatInputDialog(String title, String label, float initial, Consumer<Optional<Float>> callback) {
		super(title, label, initial, callback);
		this.buffer = new ImFloat(initial);
	}

	public static void show(String title, String label, float initial, Consumer<Optional<Float>> callback) {
		new FloatInputDialog(title, label, initial, callback).open();
	}

	@Override
	protected void renderFields() {
		imgui.ImGui.setNextItemWidth(160);
		if (imgui.ImGui.inputFloat("##value", buffer)) {
			value = buffer.get();
		}
	}
}