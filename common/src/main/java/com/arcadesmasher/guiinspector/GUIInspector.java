package com.arcadesmasher.guiinspector;

import com.arcadesmasher.guiinspector.data.rowdata.DefaultRowData;
import com.arcadesmasher.guiinspector.data.rowdata.RowData;
import com.arcadesmasher.guiinspector.input.*;
import com.arcadesmasher.guiinspector.data.nodedata.AlternatingDisplayNodeData;
import com.arcadesmasher.guiinspector.data.FriendlyDisplay;
import com.arcadesmasher.guiinspector.data.nodedata.NodeData;
import com.arcadesmasher.guiinspector.mappings.ClassMappings;
import com.arcadesmasher.guiinspector.mixin.ClickableWidgetAccessor;
import com.arcadesmasher.guiinspector.mixin.SlotAccessor;
import com.arcadesmasher.guiinspector.tree.TreePanel;
import com.arcadesmasher.guiinspector.tree.TreeWindow;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class GUIInspector implements ClientModInitializer {
	public static final String MOD_ID = "guiinspector";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ArrayList<Object> activeWidgets = new ArrayList<>();

	public static boolean selectorMode = false;

	public static TreeWindow treeWindow;
	public static TreePanel widgets;
	public static TreePanel drawCalls;

	public static TreeBuilder treeBuilder;

	public static boolean drawCallViewIsTree = true;
	public static boolean drawCallNamesAreSimple = true;

	public static boolean drawCapture = false;
	public static boolean pendingCapture = false;

	public static boolean sodiumLoaded;
	public static boolean sodiumNew;
	public static SodiumCompat sodiumCompat;

	private static Consumer<TreeSelectionEvent> selectionListener;

	public static Runnable refreshHook;
	public static Consumer<Object> removeHook;
	public static Consumer<Object> toggleVisibilityHook;

	@Override
	public void onInitializeClient() {

		sodiumLoaded = FabricLoader.getInstance().isModLoaded("sodium");
		if (sodiumLoaded) {
			Optional<ModContainer> container = FabricLoader.getInstance().getModContainer("sodium");
			if (container.isPresent()) {
				Version version = container.get().getMetadata().getVersion();

				VersionPredicate predicate;
				try {
					predicate = VersionPredicate.parse(">=0.6.0");
				} catch (VersionParsingException e) {
					throw new RuntimeException(e);
				}

				sodiumNew = predicate.test(version);
				if (sodiumNew) sodiumCompat = new SodiumCompatNew();
				else sodiumCompat = new SodiumCompatOld();
			}
		}

		System.setProperty("java.awt.headless", "false");

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignored) {}

		treeWindow = TreeWindow.getInstance();
		widgets = treeWindow.addTab("Widgets");
		drawCalls = treeWindow.addTab("Draw Calls");

		widgets.showSideMenu();
		drawCalls.hideSideMenu();

		selectionListener = e -> {
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
				widgets.addSideMenuEntry(new DefaultRowData<>(widget::getX, widget::setX, "x: ", Integer.class));
				widgets.addSideMenuEntry(new DefaultRowData<>(widget::getY, widget::setY, "y: ", Integer.class));
				addSideMenuWidthHeightSetterEntry(widget, WidthHeightRegistry.Enum.WIDTH);
				addSideMenuWidthHeightSetterEntry(widget, WidthHeightRegistry.Enum.HEIGHT);
			} else if (object instanceof DimensionedMirror dimensionedMirror) {
				Dim2iAccessor dim2iAccessor = dimensionedMirror.getDim2i();
				widgets.addSideMenuEntry("");
				widgets.addSideMenuEntry(new DefaultRowData<>(dimensionedMirror::getX, dim2iAccessor::setX, "x: ", Integer.class));
				widgets.addSideMenuEntry(new DefaultRowData<>(dimensionedMirror::getY, dim2iAccessor::setY, "y: ", Integer.class));
				widgets.addSideMenuEntry(new DefaultRowData<>(dimensionedMirror::getWidth, dim2iAccessor::setWidth, "width: ", Integer.class));
				widgets.addSideMenuEntry(new DefaultRowData<>(dimensionedMirror::getHeight, dim2iAccessor::setHeight, "height: ", Integer.class));
			}
			if (sodiumLoaded) sodiumCompat.addSodiumEntries(widgets, object);
			if (object instanceof ClickableWidget clickableWidget) {
				ClickableWidgetAccessor clickableWidgetAccessor = (ClickableWidgetAccessor) clickableWidget;
				widgets.addSideMenuEntry("");
				widgets.addSideMenuEntry(new DefaultRowData<>(() -> clickableWidget.visible, visible -> clickableWidget.visible = visible, "Visible: ", Boolean.class));
				widgets.addSideMenuEntry(new DefaultRowData<>(() -> clickableWidget.active, active -> clickableWidget.active = active, "Active: ", Boolean.class));
				widgets.addSideMenuEntry(new DefaultRowData<>(clickableWidgetAccessor::acquireAlpha, clickableWidget::setAlpha, "Alpha: ", Float.class));
				widgets.addSideMenuEntry("Selected: " + clickableWidget.isSelected());
				VersionBridge.addSideMenuEntry_ClickableWidget_Interactable(clickableWidget);
				widgets.addSideMenuEntry(new DefaultRowData<>(clickableWidget::getMessage, clickableWidget::setMessage, "Raw message: ", Text.class));
				widgets.addSideMenuEntry("Message: " + clickableWidget.getMessage().getString());
			}
			if (object instanceof Element element) {
				widgets.addSideMenuEntry("");
				widgets.addSideMenuEntry(new DefaultRowData<>(element::isFocused, element::setFocused, "Focused: ", Boolean.class));
			}
			if (object instanceof Slot slot) {
				SlotAccessor slotAccessor = (SlotAccessor) slot;
				widgets.addSideMenuEntry("");
				widgets.addSideMenuEntry(new DefaultRowData<>(slot::getIndex, warnBreakingSetter(slotAccessor::setIndex), "index: ", Integer.class));
				widgets.addSideMenuEntry(new DefaultRowData<>(() -> slot.x, slotAccessor::setX, "x: ", Integer.class));
				widgets.addSideMenuEntry(new DefaultRowData<>(() -> slot.y, slotAccessor::setY, "y: ", Integer.class));
				widgets.addSideMenuEntry("Contains: ");
				widgets.addSideMenuEntry(new DefaultRowData<>(() -> slot.inventory.getStack(slotAccessor.getIndex()), itemStack -> slot.inventory.setStack(slotAccessor.getIndex(), itemStack), "ItemStack: ", ItemStack.class));
				widgets.addSideMenuEntry(new DefaultRowData<>(() -> slot.inventory.getStack(slotAccessor.getIndex()).getCount(), value -> slot.inventory.getStack(slotAccessor.getIndex()).setCount(value), "Count: ", Integer.class));
			}
		};

		widgets.addSelectionListener(selectionListener::accept);

		JButton removeBtn = new JButton("Remove");
		JButton modifyValueBtn = new JButton("Modify Value");
		JButton refreshBtn = new JButton("Refresh");
		JButton inspectBtn = new JButton("Inspect");

		widgets.getToolbar().add(removeBtn);
		widgets.getToolbar().add(modifyValueBtn);
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

		@SuppressWarnings("unchecked")
		Map<Class<?>, BiFunction<RowData<?>, Frame, Optional<?>>> handlers = Map.of(
				Integer.class, (rd, frame) -> IntInputDialog.show(frame, "Modify Value", rd.label(), ((RowData<Integer>) rd).getter().get()),
				Float.class, (rd, frame) -> FloatInputDialog.show(frame, "Modify Value", rd.label(), ((RowData<Float>) rd).getter().get()),
				Double.class, (rd, frame) -> DoubleInputDialog.show(frame, "Modify Value", rd.label(), ((RowData<Double>) rd).getter().get()),
				Boolean.class, (rd, frame) -> BooleanInputDialog.show(frame, "Modify Value", rd.label(), ((RowData<Boolean>) rd).getter().get()),
				String.class, (rd, frame) -> StringInputDialog.show(frame, "Modify Value", rd.label(), ((RowData<String>) rd).getter().get()),
				Text.class, (rd, frame) -> TextInputDialog.show(frame, "Modify Value", ((RowData<Text>) rd).getter().get()),
				ItemStack.class, (rd, frame) -> ItemStackInputDialog.show(frame, "Modify Value", ((RowData<ItemStack>) rd).getter().get())
		);

		modifyValueBtn.addActionListener(e -> {
			Object selectedValue = widgets.getSideMenuList().getSelectedValue();
			if (selectedValue == null) {
				JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(widgets),
						"Select an entry in the details pane to modify.", "No Selection",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			if (selectedValue instanceof RowData<?> rd) { // TODO: Close edit dialog when different option gets selected/entries are cleared. Not necessarily a problem but misleading to the user
				var handler = handlers.get(rd.type());
				if (handler != null) {
					DefaultMutableTreeNode nodeAtOpen = widgets.getSelectedNode();
					@SuppressWarnings("unchecked")
					Optional<Object> result = (Optional<Object>) handler.apply(rd, treeWindow.getFrame()); // not a leak; rd is dereferenced after lambda returns
					result.ifPresent(r -> {
						if (widgets.getSelectedNode() != nodeAtOpen) return;
						@SuppressWarnings("unchecked")
						RowData<Object> typedRow = (RowData<Object>) rd;
						typedRow.setter().accept(r);
						selectionListener.accept(null);
					});
				} else {
					JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(widgets),
							"Unhandled type: " + ClassMappings.getMappedName(rd.type()) + ". Please report this to the developer along with the error message.", "Unhandled Type",
							JOptionPane.WARNING_MESSAGE);
					LOGGER.error("Unhandled type: {}. Please report this to the developer along with the error message.", ClassMappings.getMappedName(rd.type()));
				}
			} else {
				JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(widgets),
						"Unmodifiable entry.", "Unmodifiable",
						JOptionPane.WARNING_MESSAGE);
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
		JButton namesSwitchBtn = new JButton("Detailed names");
		JButton viewSwitchBtn = new JButton("Linear view");
		JButton clearBtn = new JButton("Clear");

		drawCalls.getToolbar().add(captureBtn);
		drawCalls.getToolbar().add(namesSwitchBtn);
		drawCalls.getToolbar().add(viewSwitchBtn);
		drawCalls.getToolbar().add(clearBtn);

		captureBtn.addActionListener(e -> {
			if (JOptionPane.showConfirmDialog(
					SwingUtilities.getWindowAncestor(drawCalls),
					"This will clear the currently captured draw calls. Are you sure?",
					"Confirmation",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE
			) == JOptionPane.CANCEL_OPTION) return;
			System.out.println("Capturing!");
			drawCalls.showSideMenu();
			drawCalls.clear();
			pendingCapture = true;
		});

		namesSwitchBtn.addActionListener(e -> {
			if (treeBuilder == null) return;
			if (drawCallNamesAreSimple) {
				drawCallNamesAreSimple = false;
				namesSwitchBtn.setText("Simple names");
			} else {
				drawCallNamesAreSimple = true;
				namesSwitchBtn.setText("Detailed names");
			}
			Enumeration<?> enumeration = drawCalls.getRootNode().depthFirstEnumeration();
			while (enumeration.hasMoreElements()) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
				if (node.getUserObject() instanceof AlternatingDisplayNodeData alternatingDisplayNodeData) {
					alternatingDisplayNodeData.setAltDisplay(drawCallNamesAreSimple);
					drawCalls.getTreeModel().nodeChanged(node);
				}
			}
		});

		viewSwitchBtn.addActionListener(e -> {
			if (treeBuilder == null) return;
			drawCalls.clear();
			if (drawCallViewIsTree) {
				drawCallViewIsTree = false;
				viewSwitchBtn.setText("Tree view");
				treeBuilder.buildFlatTree(drawCalls);
			} else {
				drawCallViewIsTree = true;
				viewSwitchBtn.setText("Linear view");
				treeBuilder.buildTree(drawCalls);
			}
			SwingUtilities.invokeLater(() -> {
				for (int i = GUIInspector.drawCalls.getTree().getRowCount() - 1; i > 0; i--) {
					GUIInspector.drawCalls.getTree().collapseRow(i);
				}
			});
		});

		clearBtn.addActionListener(e -> {
			if (JOptionPane.showConfirmDialog(
					SwingUtilities.getWindowAncestor(drawCalls),
					"This will clear the currently captured draw calls. Are you sure?",
					"Confirmation",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE
			) == JOptionPane.CANCEL_OPTION) return;
			drawCalls.hideSideMenu();
			drawCalls.clear();
		});

		treeWindow.show();

		// maybe add some sort of key combo for inspector mode later?
//		ClientTickEvents.END_CLIENT_TICK.register(client -> {
//			while (toggleKey.wasPressed()) {
//				selectorMode = !selectorMode;
//				System.out.println("GUI Inspector: " + (selectorMode ? "ON" : "OFF"));
//			}
//		});

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> afterInit(client, screen));
	}

	private <T> Consumer<T> warnBreakingSetter(Consumer<T> setter) {
		return object -> {
			if (JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(widgets),
					"This may potentially break things. Are you sure?",
					"Warning",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
				setter.accept(object);
			}
		};
	}

	private void afterInit(MinecraftClient client, Screen screen) {

		removeHook = (object) -> {
			if (!(object instanceof Element target)) return;

			Consumer<ParentElement> traverseAndRemove = new Consumer<>() {
				@Override
				public void accept(ParentElement parent) {
					Iterator<? extends Element> it = parent.children().iterator();
					while (it.hasNext()) {
						Element child = it.next();

						if (child == target) {
							try {
								it.remove();
							} catch (UnsupportedOperationException e) {
								JOptionPane.showMessageDialog(
										null,
										"The element cannot be removed. The containing list is immutable.",
										"Cannot Remove",
										JOptionPane.WARNING_MESSAGE
								);
								return;
							}
						} else if (child instanceof ParentElement pe) {
							accept(pe);
						}
					}
				}
			};

			traverseAndRemove.accept(screen);
		};
		toggleVisibilityHook = (object) -> {
			if (object instanceof ClickableWidget widget) widget.visible = !widget.visible;
		};

		refreshHook = () -> {

			System.out.println("refresh");

			widgets.clear();

			if (client.currentScreen != screen) {
				if (client.currentScreen == null) return;

				afterInit(client, client.currentScreen);

				return;
			}

			widgets.getRootNode().setUserObject(screen);

			treeWidgets(screen.children(), widgets.getRootNode());
			if (screen instanceof HandledScreen<?> handledScreen) {
				for (Slot slot : handledScreen.getScreenHandler().slots) {
					widgets.addNode(slot);
				}
			}
		};

		refreshHook.run();

		ScreenEvents.afterRender(screen).register((scr, context, mouseX, mouseY, tickDelta) -> {

			if (!GUIInspector.selectorMode) return;

			for (Object object : GUIInspector.findAllDeepestWidgetsAt(scr.children(), mouseX, mouseY)) {
				if (object instanceof Widget widget) {
					GUIInspector.drawOutline(context, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
				} else if (GUIInspector.sodiumLoaded && sodiumCompat.hasDimensions(object)) {
					int[] dimensions = sodiumCompat.getDimensions(object);
					GUIInspector.drawOutline(context, dimensions[0], dimensions[1], dimensions[2], dimensions[3]);
				}
			}
		});

		ScreenEvents.remove(screen).register((scr) -> {
			widgets.getRootNode().setUserObject((FriendlyDisplay) () -> "No screen active");
			widgets.clear();
			refreshHook = null;
			removeHook = null;
			toggleVisibilityHook = null;
		});
	}

	private static void addSideMenuWidthHeightSetterEntry(Widget widget, WidthHeightRegistry.Enum widthHeight) {
		Consumer<Integer> setter = WidthHeightRegistry.getSetter(widthHeight, widget);
		widgets.addSideMenuEntry(new DefaultRowData<>(widthHeight == WidthHeightRegistry.Enum.WIDTH ? widget::getWidth : widget::getHeight, setter, widthHeight.getValue() + ": ", Integer.class));
	}

	public static List<Object> findAllDeepestWidgetsAt(List<?> widgets, double x, double y) {
		List<Object> deepestWidgets = new ArrayList<>();

		for (Object obj : widgets) {
			if (obj instanceof Widget widget && x >= widget.getX() && x <= (widget.getX() + widget.getWidth())
											&&	y >= widget.getY() && y <= (widget.getY() + widget.getHeight())) {
				if (widget instanceof ParentElement parent) {
					// Recursively check children
					List<Object> childrenDeepest = findAllDeepestWidgetsAt(parent.children(), x, y);
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
			} else if (sodiumLoaded && sodiumCompat.hasDimensions(obj)) {
				int[] dimensions = sodiumCompat.getDimensions(obj);
				if (x >= dimensions[0] && x <= (dimensions[0] + dimensions[2])
				&&	y >= dimensions[1] && y <= (dimensions[1] + dimensions[3])) {
					if (obj instanceof ParentElement parent) {
						List<Object> childrenDeepest = findAllDeepestWidgetsAt(parent.children(), x, y);
						if (!childrenDeepest.isEmpty()) {
							deepestWidgets.addAll(childrenDeepest);
						} else {
							deepestWidgets.add(obj);
						}
					} else {
						deepestWidgets.add(obj);
					}

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
			} else if (sodiumLoaded && sodiumCompat.hasDimensions(object)) {
				int[] dimensions = sodiumCompat.getDimensions(object);
				System.out.println("  ".repeat(depth) + "x: " + dimensions[0]);
				System.out.println("  ".repeat(depth) + "y: " + dimensions[1]);
				System.out.println("  ".repeat(depth) + "width: " + dimensions[2]);
				System.out.println("  ".repeat(depth) + "height: " + dimensions[3]);
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

	public static void MouseMixin_onMouseButton(Object self, long window, int button, int action, CallbackInfo ci) {

		if (!selectorMode) return;
		if (button != 0) return;
		if (action != 1) return;

		Mouse mouse = (Mouse) self;

		if (mouse.client.currentScreen == null) return;
		if (mouse.client.getWindow().getHandle() != window) return;

		activeWidgets.clear();
		activeWidgets.ensureCapacity(mouse.client.currentScreen.children().size());

		activeWidgets.addAll(findAllDeepestWidgetsAt(mouse.client.currentScreen.children(), mouse.getX(), mouse.getY()));

		selectorMode = false;
		System.out.println("GUI Inspector: OFF");

//				logWidgets(activeWidgets);
		if (activeWidgets.isEmpty()) {
			widgets.setSelectedNode(widgets.getRootNode());
		} else {
			DefaultMutableTreeNode selectedWidget = widgets.findNode(activeWidgets.get(0));
			widgets.setSelectedNode(selectedWidget, success -> {
				if (!success) {
					refreshHook.run();
				}
				widgets.setSelectedNode(selectedWidget, s -> {
					if (!s) {
						System.out.println(ClassMappings.getMappedName(mouse.client.currentScreen));
//								logWidgets(scr.children());
					}
				});
			});
		}
		activeWidgets.clear();
		ci.cancel();
	}
}