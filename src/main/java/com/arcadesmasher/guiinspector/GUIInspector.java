package com.arcadesmasher.guiinspector;

import com.arcadesmasher.guiinspector.data.FriendlyDisplay;
import com.arcadesmasher.guiinspector.data.NodeData;
import com.arcadesmasher.guiinspector.mappings.ClassMappings;
import com.arcadesmasher.guiinspector.tree.TreePanel;
import com.arcadesmasher.guiinspector.tree.TreeWindow;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.Widget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GUIInspector implements ClientModInitializer {
	public static final String MOD_ID = "guiinspector";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ArrayList<Widget> activeWidgets = new ArrayList<>();

	public static boolean selectorMode = false;

	public static TreeWindow treeWindow;
	public static TreePanel widgets;
	public static TreePanel drawCalls;

	public static boolean drawCapture = false;
	public static boolean pendingCapture = false;

	public Runnable refreshHook;
	public Consumer<Object> removeHook;
	public Consumer<Object> toggleVisibilityHook;

	@Override
	public void onInitializeClient() {

		System.setProperty("java.awt.headless", "false");

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignored) {}

		treeWindow = TreeWindow.getInstance();
		widgets = treeWindow.addTab("Widgets");
		drawCalls = treeWindow.addTab("Draw Calls");

		widgets.showSideMenu();
		drawCalls.hideSideMenu();

		widgets.addSelectionListener(e -> {
			widgets.clearSideMenu();
			DefaultMutableTreeNode selectedNode = widgets.getSelectedNode();
			if (selectedNode == null) return;
			Object object = selectedNode.getUserObject();
			widgets.addSideMenuEntry("Class name: " + ClassMappings.getMappedName(object));
			widgets.addSideMenuEntry("Unmapped class name: " + object.getClass());
			if (object instanceof ParentElement parentElement) {
				widgets.addSideMenuEntry("Children: " + parentElement.children().size());
			}
			if (object instanceof Widget widget) {
				widgets.addSideMenuEntry("");
				widgets.addSideMenuEntry("x: " + widget.getX());
				widgets.addSideMenuEntry("y: " + widget.getY());
				widgets.addSideMenuEntry("width: " + widget.getWidth());
				widgets.addSideMenuEntry("height: " + widget.getHeight());
			}
			if (object instanceof ClickableWidget clickableWidget) {
				widgets.addSideMenuEntry("");
				widgets.addSideMenuEntry("Visible: " + clickableWidget.visible);
				widgets.addSideMenuEntry("Active: " + clickableWidget.active);
				widgets.addSideMenuEntry("Alpha: " + clickableWidget.getAlpha());
				widgets.addSideMenuEntry("Focused: " + clickableWidget.isFocused());
				widgets.addSideMenuEntry("Selected: " + clickableWidget.isSelected());
				widgets.addSideMenuEntry("Interactable: " + clickableWidget.isInteractable());
				widgets.addSideMenuEntry("Raw message: " + clickableWidget.getMessage());
				widgets.addSideMenuEntry("Message: " + clickableWidget.getMessage().getString());
			}
		});

		JButton removeBtn = new JButton("Remove");
		JButton toggleVisibilityBtn = new JButton("Toggle Visibility");
		JButton refreshBtn = new JButton("Refresh");
		JButton inspectBtn = new JButton("Inspect");

		widgets.getToolbar().add(removeBtn);
		widgets.getToolbar().add(toggleVisibilityBtn);
		widgets.getToolbar().add(refreshBtn);
		widgets.getToolbar().add(inspectBtn);

		removeBtn.addActionListener(e -> {
			DefaultMutableTreeNode selected = widgets.getSelectedNode();
			if (selected == null || selected == widgets.getRootNode()) {
				JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(widgets),
						"Select a node to remove.", "No Selection",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			int result = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(widgets),
					"This may potentially break things. Are you sure?",
					"Warning",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE
			);
			if (result == JOptionPane.YES_OPTION) {
				if (removeHook != null) {
					removeHook.accept(selected.getUserObject());
				}
			}
			widgets.removeNode(selected);
		});

		toggleVisibilityBtn.addActionListener(e -> {
			DefaultMutableTreeNode selected = widgets.getSelectedNode();
			if (selected == null || selected == widgets.getRootNode()) {
				JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(widgets),
						"Select a node to toggle visibility.", "No Selection",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			int result = JOptionPane.showConfirmDialog(
					SwingUtilities.getWindowAncestor(widgets),
					"This may cause unintended behavior. Are you sure?",
					"Warning",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE
			);
			if (result == JOptionPane.YES_OPTION) {
				if (toggleVisibilityHook != null) {
					toggleVisibilityHook.accept(selected.getUserObject());
				}
			}
		});

		inspectBtn.addActionListener(e -> {
			selectorMode = !selectorMode;
			System.out.println("GUI Inspector: " + (selectorMode ? "ON" : "OFF"));
		});

		refreshBtn.addActionListener(e -> {
			if (refreshHook != null) {
				refreshHook.run();
			}
		});


		drawCalls.getRootNode().setUserObject((FriendlyDisplay) () -> "Draw Calls");

		drawCalls.addSelectionListener(e -> {
			drawCalls.clearSideMenu();
			DefaultMutableTreeNode selectedNode = drawCalls.getSelectedNode();
			if (selectedNode == null || selectedNode.getUserObject() == null) return;
			if (selectedNode.getUserObject() instanceof NodeData nodeData) {
				drawCalls.setSideMenuEntries(List.of(nodeData.details()));
			}
		});

		JButton captureBtn = new JButton("Capture");

		drawCalls.getToolbar().add(captureBtn);

		captureBtn.addActionListener(e -> {
			System.out.println("Capturing!");
			GUIInspector.drawCalls.showSideMenu();
			GUIInspector.drawCalls.clear();
			pendingCapture = true;
		});

		treeWindow.show();

//		ClientTickEvents.END_CLIENT_TICK.register(client -> {
//			while (toggleKey.wasPressed()) {
//				selectorMode = !selectorMode;
//				System.out.println("GUI Inspector: " + (selectorMode ? "ON" : "OFF"));
//			}
//		});

		ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {

			ScreenMouseEvents.allowMouseDrag(screen).register((scr, click, i, j) -> !selectorMode);

			ScreenMouseEvents.allowMouseRelease(screen).register((scr, click) -> !selectorMode);

			ScreenMouseEvents.allowMouseClick(screen).register((scr, click) -> {
				if (selectorMode) {
					activeWidgets.clear();
					activeWidgets.ensureCapacity(screen.children().size());

                    activeWidgets.addAll(findAllDeepestWidgetsAt(screen.children(), click.x(), click.y()));

					selectorMode = false;
					System.out.println("GUI Inspector: OFF");

					logWidgets(activeWidgets);
					if (activeWidgets.isEmpty()) {
						widgets.setSelectedNode(widgets.getRootNode());
					} else {
						DefaultMutableTreeNode selectedWidget = widgets.findNode(activeWidgets.getFirst());
						widgets.setSelectedNode(selectedWidget, success -> {
							if (!success) {
								refreshHook.run();
							}
							widgets.setSelectedNode(selectedWidget, s -> {
								if (!s) {
									System.out.println(ClassMappings.getMappedName(screen));
									logWidgets(screen.children());
								}
							});
						});
					}
					activeWidgets.clear();
					return false;
				}
                return true;
            });

		});

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {

			afterInit(client, screen);
		});
	}

	private void afterInit(MinecraftClient client, Screen screen) {

		removeHook = (object) -> {
			if (object instanceof Element element) screen.remove(element);
		};
		toggleVisibilityHook = (object) -> {
			if (object instanceof ClickableWidget widget) widget.visible = !widget.visible;
		};

		refreshHook = () -> { // TODO: This refresh thing is problematic. If a screen child is removed, it will still be referenced until refresh is called. I'm not sure if the GC will properly collect it once we refresh, so put a WeakReference on it to check this, if it doesn't GC then fix the method to auto-refresh when children are removed.

			System.out.println("refresh");

			widgets.clear();

			if (client.currentScreen != screen) {
				if (client.currentScreen == null) return;

				afterInit(client, client.currentScreen);

				return;
			}

			widgets.getRootNode().setUserObject(screen);

			treeWidgets(screen.children(), widgets.getRootNode());
		};

		refreshHook.run();

		ScreenEvents.afterRender(screen).register((scr, context, mouseX, mouseY, tickDelta) -> {
			DefaultMutableTreeNode selectedWidget = GUIInspector.widgets.getSelectedNode();
			if (selectedWidget != null && selectedWidget.getUserObject() instanceof Widget widget) {
				GUIInspector.drawOutline(context, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
			}

			DefaultMutableTreeNode selectedDrawCall = GUIInspector.drawCalls.getSelectedNode();
			if (selectedDrawCall != null) {
				Object userObj = selectedDrawCall.getUserObject();
				if (userObj instanceof NodeData data) data.drawOutline(context);
			}

			if (!GUIInspector.selectorMode) return;

			for (Widget widget : GUIInspector.findAllDeepestWidgetsAt(screen.children(), mouseX, mouseY)) {
				GUIInspector.drawOutline(context, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
			}
		});

		ScreenEvents.remove(screen).register((scr) -> {
			widgets.getRootNode().setUserObject(null);
			refreshHook = null;
			removeHook = null;
		});
	}

	public static List<Widget> findAllDeepestWidgetsAt(List<?> widgets, double x, double y) {
		List<Widget> deepestWidgets = new ArrayList<>();

		for (Object obj : widgets) {
			if (obj instanceof Widget widget && x >= widget.getX() && x <= (widget.getX() + widget.getWidth())
					&& y >= widget.getY() && y <= (widget.getY() + widget.getHeight())) {
				if (widget instanceof ParentElement parent) {
					// Recursively check children
					List<Widget> childrenDeepest = findAllDeepestWidgetsAt(parent.children(), x, y);
					if (!childrenDeepest.isEmpty()) {
						deepestWidgets.addAll(childrenDeepest);
					} else {
						// No children contain the point, so this widget is deepest here
						deepestWidgets.add(widget);
					}
				} else {
					// Not a parent, so it's deepest
					deepestWidgets.add(widget);
				}
			}
		}

		return deepestWidgets;
	}

	private static void treeWidgets(List<?> objects, DefaultMutableTreeNode parent) {

		for (Object object : objects) {
			DefaultMutableTreeNode child = widgets.addChildNode(parent, object);
			if (object instanceof ParentElement parentElement) {
				treeWidgets(parentElement.children(), child);
			}
		}
	}

	private static void logWidgets(List<?> objects) {
		logWidgets(objects, 0);
	}

	private static void logWidgets(List<?> objects, int depth) {

		for (Object object : objects) {
			System.out.println("  ".repeat(depth) + ClassMappings.getMappedName(object));
			if (object instanceof Widget widget) {
				System.out.println("  ".repeat(depth) + "x:      " + widget.getX());
				System.out.println("  ".repeat(depth) + "y:      " + widget.getY());
				System.out.println("  ".repeat(depth) + "width:  " + widget.getWidth());
				System.out.println("  ".repeat(depth) + "height: " + widget.getHeight());
			}
			if (object instanceof ParentElement parentElement) {
				System.out.println("  ".repeat(depth) + "CHILDREN:");
				logWidgets(parentElement.children(), depth + 1);
			}
			System.out.println("\n");
		}
	}

	public static void drawOutline(DrawContext context, int x, int y, int width, int height) {

		int red = 0xFFFF0000;

		int left = x - 1;
		int right = x + width + 1;
		int top = y - 1;
		int bottom = y + height + 1;

		// top
		context.fill(left, top, right, top + 1, red);
		// bottom
		context.fill(left, bottom - 1, right, bottom, red);
		// left
		context.fill(left, top, left + 1, bottom, red);
		// right
		context.fill(right - 1, top, right, bottom, red);
	}
}