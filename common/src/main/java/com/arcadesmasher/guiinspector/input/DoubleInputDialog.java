package com.arcadesmasher.guiinspector.input;

import imgui.type.ImDouble;

import java.util.Optional;
import java.util.function.Consumer;

public class DoubleInputDialog extends InputDialog<Double> {

	private final ImDouble buffer;

	protected DoubleInputDialog(String title, String label, double initial, Consumer<Optional<Double>> callback) {
		super(title, label, initial, callback);
		this.buffer = new ImDouble(initial);
	}

	public static void show(String title, String label, double initial, Consumer<Optional<Double>> callback) {
		new DoubleInputDialog(title, label, initial, callback).open();
	}

	@Override
	protected void renderFields() {
		imgui.ImGui.setNextItemWidth(160);
		if (imgui.ImGui.inputDouble("##value", buffer)) {
			value = buffer.get();
		}
	}
}