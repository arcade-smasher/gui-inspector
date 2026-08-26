package com.arcadesmasher.guiinspector.input;

import imgui.ImGui;
import imgui.flag.ImGuiCond;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

public final class Popups {

	private enum Kind { MESSAGE, CONFIRM }

	private record Entry(Kind kind, String title, String message, Consumer<Boolean> callback) {}

	private static final Deque<Entry> queue = new ArrayDeque<>();
	private static boolean openedThisFrame = false;

	private Popups() {}

	public static void message(String title, String message) {
		queue.add(new Entry(Kind.MESSAGE, title, message, null));
	}

	public static void confirm(String title, String message, Consumer<Boolean> callback) {
		queue.add(new Entry(Kind.CONFIRM, title, message, callback));
	}

	public static void renderActive() {
		if (queue.isEmpty()) return;

		Entry entry = queue.peek();
		String popupId = entry.title() + "##simplepopup";

		if (!openedThisFrame) {
			ImGui.openPopup(popupId);
			openedThisFrame = true;
		}

		boolean[] open = {true};
		boolean closed = false;
		boolean result = false;

		float windowWidth = ImGui.getIO().getDisplaySizeX();
		float windowHeight = ImGui.getIO().getDisplaySizeY();

		float popupWidth = Math.min(windowWidth * 0.50f, 600.0f);

		ImGui.setNextWindowPos(windowWidth * 0.5f, windowHeight * 0.5f, ImGuiCond.Appearing, 0.5f, 0.5f);

		ImGui.setNextWindowSize(popupWidth, 0.0f, ImGuiCond.Appearing);

		if (ImGui.beginPopupModal(popupId)) {
			ImGui.textWrapped(entry.message());
			ImGui.separator();

			if (entry.kind() == Kind.CONFIRM) {
				if (ImGui.button("Yes")) {
					closed = true;
					result = true;
				}

				ImGui.sameLine();

				if (ImGui.button("No")) {
					closed = true;
				}
			} else {
				if (ImGui.button("OK")) {
					closed = true;
				}
			}

			if (closed)
				ImGui.closeCurrentPopup();

			ImGui.endPopup();
		}

		if (!open[0])
			closed = true;

		if (closed) {
			queue.poll();
			openedThisFrame = false;

			if (entry.callback() != null)
				entry.callback().accept(result);
		}
	}
}