package com.arcadesmasher.guiinspector.tree;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class TreeWindow {

	private static TreeWindow instance;

	private final JFrame frame;
	private final JTabbedPane tabbedPane;
	private final Map<String, TreePanel> tabs = new LinkedHashMap<>();

	private TreeWindow() {
		tabbedPane = new JTabbedPane();

		frame = new JFrame("GUI Inspector");
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.setSize(400, 500);
		frame.setLocationRelativeTo(null);
		frame.add(tabbedPane);
	}

	public static TreeWindow getInstance() {
		if (instance == null) {
			if (SwingUtilities.isEventDispatchThread()) {
				instance = new TreeWindow();
			} else {
				try {
					SwingUtilities.invokeAndWait(() -> instance = new TreeWindow());
				} catch (Exception ex) {
					throw new RuntimeException("Failed to create TreeWindow on EDT", ex);
				}
			}
		}
		return instance;
	}

	public TreePanel addTab(String name) {
		if (tabs.containsKey(name)) {
			return tabs.get(name);
		}
		TreePanel panel = new TreePanel();
		tabs.put(name, panel);
		SwingUtilities.invokeLater(() -> tabbedPane.addTab(name, panel));
		return panel;
	}

	public TreePanel getTab(String name) {
		return tabs.get(name);
	}

	public void removeTab(String name) {
		TreePanel panel = tabs.remove(name);
		if (panel != null) {
			SwingUtilities.invokeLater(() -> tabbedPane.remove(panel));
		}
	}

	public TreePanel getSelectedTab() {
		int idx = tabbedPane.getSelectedIndex();
		if (idx < 0) return null;
		String name = tabbedPane.getTitleAt(idx);
		return tabs.get(name);
	}

	public void show() {
		SwingUtilities.invokeLater(() -> {
			frame.setVisible(true);
			frame.toFront();
		});
	}

	public void hide() {
		SwingUtilities.invokeLater(() -> frame.setVisible(false));
	}

	public void toggle() {
		SwingUtilities.invokeLater(() -> {
			if (frame.isVisible()) {
				frame.setVisible(false);
			} else {
				frame.setVisible(true);
				frame.toFront();
			}
		});
	}

	public Frame getFrame() { return frame; }
}