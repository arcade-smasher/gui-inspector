package com.arcadesmasher.guiinspector.input;

import imgui.ImGui;
import imgui.flag.ImGuiCond;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Consumer;

public final class Popups {

	private enum Kind { DIALOG, MESSAGE, CONFIRM }

	public static final class Entry {
		private final Kind kind;
		private final String title;
		private final String message;
		private final Consumer<Boolean> callback;

		private boolean closeRequested;

		private Entry(Kind kind, String title, String message, Consumer<Boolean> callback) {
			this.kind = kind;
			this.title = title;
			this.message = message;
			this.callback = callback;
		}

		public void close() {
			closeRequested = true;
		}

		public Kind kind() {
			return kind;
		}
		public String title() {
			return title;
		}
		public String message() {
			return message;
		}
		public Consumer<Boolean> callback() {
			return callback;
		}
	}

	private static final Deque<Entry> queue = new ArrayDeque<>();
	private static boolean openedThisFrame = false;

	private Popups() {}

	public static Entry dialog(String title, String message) {
		Entry entry = new Entry(Kind.DIALOG, title, message, null);
		queue.add(entry);
		return entry;
	}

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
					entry.closeRequested = true;
					result = true;
				}

				ImGui.sameLine();

				if (ImGui.button("No")) {
					entry.closeRequested = true;
				}
			} else if (entry.kind() == Kind.MESSAGE) {
				if (ImGui.button("OK")) {
					entry.closeRequested = true;
				}
			}

			if (entry.closeRequested)
				ImGui.closeCurrentPopup();

			ImGui.endPopup();
		}

		if (!open[0])
			entry.closeRequested = true;

		if (entry.closeRequested) {
			queue.poll();
			openedThisFrame = false;

			if (entry.callback() != null)
				entry.callback().accept(result);
		}
	}
}