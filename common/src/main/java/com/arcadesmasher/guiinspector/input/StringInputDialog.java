package com.arcadesmasher.guiinspector.input;

import imgui.type.ImString;

import java.util.Optional;
import java.util.function.Consumer;

public class StringInputDialog extends InputDialog<String> {

	private final ImString buffer;

	protected StringInputDialog(String title, String label, String initial, Consumer<Optional<String>> callback) {
		super(title, label, initial, callback);
		this.buffer = new ImString(initial, 256);
	}

	public static void show(String title, String label, String initial, Consumer<Optional<String>> callback) {
		new StringInputDialog(title, label, initial, callback).open();
	}

	@Override
	protected void renderFields() {
		imgui.ImGui.setNextItemWidth(160);
		if (imgui.ImGui.inputText("##value", buffer)) {
			value = buffer.get();
		}
	}
}