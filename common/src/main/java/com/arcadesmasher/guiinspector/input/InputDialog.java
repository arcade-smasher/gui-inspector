package com.arcadesmasher.guiinspector.input;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class InputDialog<T> {

	private static final Deque<InputDialog<?>> queue = new ArrayDeque<>();
	private static boolean popupOpenedThisFrame = false;

	private final String title;
	private final String label;
	protected T value;
	private final Consumer<Optional<T>> callback;

	protected InputDialog(String title, String label, T initial, Consumer<Optional<T>> callback) {
		this.title = title;
		this.label = label;
		this.value = initial;
		this.callback = callback;
	}

	public void open() {
		queue.add(this);
	}

	protected abstract void renderFields();

	protected boolean isValid() {
		return true;
	}

	public static void renderActive() {
		if (queue.isEmpty()) return;
		InputDialog<?> dialog = queue.peek();

		String popupId = dialog.title + "##inputdialog";
		if (!popupOpenedThisFrame) {
			ImGui.openPopup(popupId);
			popupOpenedThisFrame = true;
		}

		boolean[] open = {true};
		boolean closed = false;
		boolean submitted = false;

		if (ImGui.beginPopupModal(popupId)) {
			ImGui.text(dialog.label);
			ImGui.sameLine();
			dialog.renderFields();
			ImGui.separator();

			boolean valid = dialog.isValid();
			if (!valid) ImGui.beginDisabled();
			if (ImGui.button("OK")) {
				closed = true;
				submitted = true;
			}
			if (!valid) ImGui.endDisabled();
			ImGui.sameLine();
			if (ImGui.button("Cancel")) {
				closed = true;
			}

			if (closed) ImGui.closeCurrentPopup();
			ImGui.endPopup();
		}

		if (!open[0]) closed = true;

		if (closed) {
			queue.poll();
			popupOpenedThisFrame = false;
			finish(dialog, submitted);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> void finish(InputDialog<?> dialog, boolean submitted) {
		InputDialog<T> typed = (InputDialog<T>) dialog;
		typed.callback.accept(submitted ? Optional.ofNullable(typed.value) : Optional.empty());
	}
}