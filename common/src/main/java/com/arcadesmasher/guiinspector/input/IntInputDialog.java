package com.arcadesmasher.guiinspector.input;

import imgui.type.ImInt;

import java.util.Optional;
import java.util.function.Consumer;

public class IntInputDialog extends InputDialog<Integer> {

	private final ImInt buffer;

	protected IntInputDialog(String title, String label, int initial, Consumer<Optional<Integer>> callback) {
		super(title, label, initial, callback);
		this.buffer = new ImInt(initial);
	}

	public static void show(String title, String label, int initial, Consumer<Optional<Integer>> callback) {
		new IntInputDialog(title, label, initial, callback).open();
	}

	@Override
	protected void renderFields() {
		imgui.ImGui.setNextItemWidth(160);
		if (imgui.ImGui.inputInt("##value", buffer)) {
			value = buffer.get();
		}
	}
}