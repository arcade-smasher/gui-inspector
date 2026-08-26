package com.arcadesmasher.guiinspector.tree;

import com.arcadesmasher.guiinspector.GUIInspector;
import com.arcadesmasher.guiinspector.input.InputDialog;
import com.arcadesmasher.guiinspector.input.Popups;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;

import java.util.LinkedHashMap;
import java.util.Map;

public class TreeWindow {

	private static volatile TreeWindow instance;

	private final IsolatedGLWindow glWindow;
	private final Map<String, TreePanel> tabs = new LinkedHashMap<>();
	private volatile String selectedTab;

	private TreeWindow() {
		glWindow = new IsolatedGLWindow();
		glWindow.setContentRenderer(this::render);
	}

	public static TreeWindow getInstance() {
		if (instance == null) {
			synchronized (TreeWindow.class) {
				if (instance == null) {
					instance = new TreeWindow();
				}
			}
		}
		return instance;
	}

	public void start() {
		glWindow.start();
	}

	public void stop() {
		glWindow.stop();
	}

	public TreePanel addTab(String name) {
		synchronized (tabs) {
			TreePanel existing = tabs.get(name);
			if (existing != null) return existing;
			TreePanel panel = new TreePanel();
			tabs.put(name, panel);
			if (selectedTab == null) selectedTab = name;
			return panel;
		}
	}

	public TreePanel getTab(String name) {
		synchronized (tabs) {
			return tabs.get(name);
		}
	}

	public void removeTab(String name) {
		synchronized (tabs) {
			tabs.remove(name);
			if (name.equals(selectedTab)) {
				selectedTab = tabs.keySet().stream().findFirst().orElse(null);
			}
		}
	}

	public TreePanel getSelectedTab() {
		synchronized (tabs) {
			return selectedTab == null ? null : tabs.get(selectedTab);
		}
	}

	public IsolatedGLWindow getGlWindow() {
		return glWindow;
	}

	public void show() {
		glWindow.show();
	}

	public void hide() {
		glWindow.hide();
	}

	public void toggle() {
		glWindow.toggleVisible();
	}

	private void render() {
		ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
		ImGui.setNextWindowSize(ImGui.getIO().getDisplaySizeX(), ImGui.getIO().getDisplaySizeY(), ImGuiCond.Always);

		ImGui.begin("GUI Inspector", ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize);

		float bottomPaneHeight = 100.0f;
		float availableHeight = ImGui.getContentRegionAvailY();

		if (ImGui.beginChild("##main", 0, availableHeight - bottomPaneHeight, false)) {
			synchronized (tabs) {
				if (ImGui.beginTabBar("##tabs")) {
					for (Map.Entry<String, TreePanel> entry : tabs.entrySet()) {
						if (ImGui.beginTabItem(entry.getKey())) {
							selectedTab = entry.getKey();
							entry.getValue().render();
							ImGui.endTabItem();
						}
					}
					ImGui.endTabBar();
				}
			}

			ImGui.endChild();
		}

		if (ImGui.beginChild("##misc", 0, bottomPaneHeight, true)) {
			ImGui.text("Miscellaneous");
			ImGui.separator();
			ImGui.text("Frame: " + GUIInspector.totalFramesRendered);
			ImGui.text("Draw calls: " + GUIInspector.totalDrawCalls);
			ImGui.text("Draw calls (fill): " + GUIInspector.totalFillDrawCalls);
			ImGui.text("Draw calls (text): " + GUIInspector.totalTextDrawCalls);
			ImGui.text("Draw calls (blit): " + GUIInspector.totalTexturedQuadDrawCalls);
			ImGui.text("Draw calls (tiledBlit): " + GUIInspector.totalTiledTexturedQuadDrawCalls);
			ImGui.text("Draw calls (item): " + GUIInspector.totalItemDrawCalls);
			ImGui.separator();

			ImGui.endChild();
		}

		ImGui.end();

		Popups.renderActive();
		InputDialog.renderActive();
	}
}