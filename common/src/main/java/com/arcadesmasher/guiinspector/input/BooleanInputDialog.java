package com.arcadesmasher.guiinspector.input;

import imgui.type.ImBoolean;

import java.util.Optional;
import java.util.function.Consumer;

public class BooleanInputDialog extends InputDialog<Boolean> {

	private final ImBoolean buffer;

	protected BooleanInputDialog(String title, String label, boolean initial, Consumer<Optional<Boolean>> callback) {
		super(title, label, initial, callback);
		this.buffer = new ImBoolean(initial);
	}

	public static void show(String title, String label, boolean initial, Consumer<Optional<Boolean>> callback) {
		new BooleanInputDialog(title, label, initial, callback).open();
	}

	@Override
	protected void renderFields() {
		imgui.ImGui.setNextItemWidth(160);
		if (imgui.ImGui.checkbox("##value", buffer)) {
			value = buffer.get();
		}
	}
}