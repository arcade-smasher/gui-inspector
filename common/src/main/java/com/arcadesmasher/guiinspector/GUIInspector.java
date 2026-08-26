package com.arcadesmasher.guiinspector;

import com.arcadesmasher.guiinspector.Dim2iAccessor;
import com.arcadesmasher.guiinspector.DimensionedMirror;
import com.arcadesmasher.guiinspector.WidthHeightRegistry;
import com.arcadesmasher.guiinspector.data.FriendlyDisplay;
import com.arcadesmasher.guiinspector.data.nodedata.AlternatingDisplayNodeData;
import com.arcadesmasher.guiinspector.data.nodedata.NodeData;
import com.arcadesmasher.guiinspector.data.nodedata.TextureSnapshot;
import com.arcadesmasher.guiinspector.data.rowdata.DefaultRowData;
import com.arcadesmasher.guiinspector.data.rowdata.RowData;
import com.arcadesmasher.guiinspector.input.*;
import com.arcadesmasher.guiinspector.mappings.MappingsResolver;
import com.arcadesmasher.guiinspector.mixin.ClickableWidgetAccessor;
import com.arcadesmasher.guiinspector.mixin.SlotAccessor;
import com.arcadesmasher.guiinspector.sodiumcompat.SodiumCompat;
import com.arcadesmasher.guiinspector.sodiumcompat.SodiumCompatNew;
import com.arcadesmasher.guiinspector.sodiumcompat.SodiumCompatOld;
import com.arcadesmasher.guiinspector.tree.TreeBuilder;
import com.arcadesmasher.guiinspector.tree.TreePanel;
import com.arcadesmasher.guiinspector.tree.TreeWindow;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiStyleVar;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.glDeleteTextures;

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
	public static boolean useDetailedDrawCallNames = false;

	public static boolean drawCapture = false;
	public static boolean pendingCapture = false;
	public static long totalFramesRendered = 0;
	public static int totalDrawCalls = 0;
	public static int totalFillDrawCalls = 0;
	public static int totalTextDrawCalls = 0;
	public static int totalTexturedQuadDrawCalls = 0;
	public static int totalTiledTexturedQuadDrawCalls = 0;
	public static int totalItemDrawCalls = 0;

	public static boolean sodiumLoaded;
	public static boolean sodiumNew;
	public static SodiumCompat sodiumCompat;

	private static Consumer<TreeSelectionEvent> selectionListener;

	public static Runnable refreshHook;
	public static Consumer<Object> removeHook;
	public static Consumer<Object> toggleVisibilityHook;

	public static MappingsResolver mappingsResolver = new MappingsResolver();
	public static boolean usingYarnMappings = true;
	private final Object mappingLock = new Object();

	@SuppressWarnings("unchecked")
	private static final Map<Class<?>, BiConsumer<RowData<?>, Consumer<Optional<Object>>>> modifyValueHandlers = Map.of(
			Integer.class, (rd, cb) -> IntInputDialog.show("Modify Value", rd.label(), ((RowData<Integer>) rd).getter().get(),
					result -> cb.accept(result.map(v -> (Object) v))),
			Float.class, (rd, cb) -> FloatInputDialog.show("Modify Value", rd.label(), ((RowData<Float>) rd).getter().get(),
					result -> cb.accept(result.map(v -> (Object) v))),
			Double.class, (rd, cb) -> DoubleInputDialog.show("Modify Value", rd.label(), ((RowData<Double>) rd).getter().get(),
					result -> cb.accept(result.map(v -> (Object) v))),
			Boolean.class, (rd, cb) -> BooleanInputDialog.show("Modify Value", rd.label(), ((RowData<Boolean>) rd).getter().get(),
					result -> cb.accept(result.map(v -> (Object) v))),
			String.class, (rd, cb) -> StringInputDialog.show("Modify Value", rd.label(), ((RowData<String>) rd).getter().get(),
					result -> cb.accept(result.map(v -> (Object) v))),
			Component.class, (rd, cb) -> TextInputDialog.show("Modify Value", rd.label(), ((RowData<Component>) rd).getter().get(),
					result -> cb.accept(result.map(v -> (Object) v))),
			ItemStack.class, (rd, cb) -> ItemStackInputDialog.show("Modify Value", rd.label(), ((RowData<ItemStack>) rd).getter().get(),
					result -> cb.accept(result.map(v -> (Object) v)))
	);

	@Override
	public void onInitializeClient() {

		try {
			mappingsResolver.loadYarn();
		} catch (MappingsResolver.MappingsException e) {
			throw new RuntimeException(e);
		}

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
			widgets.addSideMenuEntry("Class name: " + getMappedClassName(object));
			widgets.addSideMenuEntry("Unmapped class name: " + object.getClass());
			if (object instanceof ContainerEventHandler parentElement) {
				widgets.addSideMenuEntry("Children: " + parentElement.children().size());
			}
			if (object instanceof LayoutElement widget) {
				widgets.addSideMenuEntry("");
				widgets.addSideMenuEntry(new DefaultRowData<>(widget::getX, widget::setX, "x: ", Integer.class));
				widgets.addSideMenuEntry(new DefaultRowData<>(widget::getY, widget::setY, "y: ", Integer.class));
				addSideMenuWidthHeightSetterEntry(widget, com.arcadesmasher.guiinspector.WidthHeightRegistry.Enum.WIDTH);
				addSideMenuWidthHeightSetterEntry(widget, com.arcadesmasher.guiinspector.WidthHeightRegistry.Enum.HEIGHT);
			} else if (object instanceof com.arcadesmasher.guiinspector.DimensionedMirror dimensionedMirror) {
				com.arcadesmasher.guiinspector.Dim2iAccessor dim2iAccessor = dimensionedMirror.getDim2i();
				widgets.addSideMenuEntry("");
				widgets.addSideMenuEntry(new DefaultRowData<>(dimensionedMirror::getX, dim2iAccessor::setX, "x: ", Integer.class));
				widgets.addSideMenuEntry(new DefaultRowData<>(dimensionedMirror::getY, dim2iAccessor::setY, "y: ", Integer.class));
				widgets.addSideMenuEntry(new DefaultRowData<>(dimensionedMirror::getWidth, dim2iAccessor::setWidth, "width: ", Integer.class));
				widgets.addSideMenuEntry(new DefaultRowData<>(dimensionedMirror::getHeight, dim2iAccessor::setHeight, "height: ", Integer.class));
			}
			if (sodiumLoaded) sodiumCompat.addSodiumEntries(widgets, object);
			if (object instanceof AbstractWidget clickableWidget) {
				ClickableWidgetAccessor clickableWidgetAccessor = (ClickableWidgetAccessor) clickableWidget;
				widgets.addSideMenuEntry("");
				widgets.addSideMenuEntry(new DefaultRowData<>(() -> clickableWidget.visible, visible -> clickableWidget.visible = visible, "Visible: ", Boolean.class));
				widgets.addSideMenuEntry(new DefaultRowData<>(() -> clickableWidget.active, active -> clickableWidget.active = active, "Active: ", Boolean.class));
				widgets.addSideMenuEntry(new DefaultRowData<>(clickableWidgetAccessor::acquireAlpha, clickableWidget::setAlpha, "Alpha: ", Float.class));
				//#if _1_20_1
				//$$ GUIInspector.widgets.addSideMenuEntry("Narratable: " + clickableWidget.isActive());
				//#elif _1_21_11
				GUIInspector.widgets.addSideMenuEntry("Interactable: " + clickableWidget.isActive());
				//#endif
				widgets.addSideMenuEntry(new DefaultRowData<>(clickableWidget::getMessage, clickableWidget::setMessage, "Raw message: ", Component.class));
				widgets.addSideMenuEntry("Message: " + clickableWidget.getMessage().getString());
			}
			if (object instanceof GuiEventListener element) {
				widgets.addSideMenuEntry("");
				widgets.addSideMenuEntry(new DefaultRowData<>(element::isFocused, element::setFocused, "Focused: ", Boolean.class));
			}
			if (object instanceof Slot slot) {
				SlotAccessor slotAccessor = (SlotAccessor) slot;
				widgets.addSideMenuEntry("");
				widgets.addSideMenuEntry(new DefaultRowData<>(slot::getContainerSlot, warnBreakingSetter(slotAccessor::setIndex), "index: ", Integer.class));
				widgets.addSideMenuEntry(new DefaultRowData<>(() -> slot.x, slotAccessor::setX, "x: ", Integer.class));
				widgets.addSideMenuEntry(new DefaultRowData<>(() -> slot.y, slotAccessor::setY, "y: ", Integer.class));
				widgets.addSideMenuEntry("Contains: ");
				widgets.addSideMenuEntry(new DefaultRowData<>(() -> slot.container.getItem(slotAccessor.getIndex()), itemStack -> slot.container.setItem(slotAccessor.getIndex(), itemStack), "ItemStack: ", ItemStack.class));
				widgets.addSideMenuEntry(new DefaultRowData<>(() -> slot.container.getItem(slotAccessor.getIndex()).getCount(), value -> slot.container.getItem(slotAccessor.getIndex()).setCount(value), "Count: ", Integer.class));
			}
		};

		widgets.addSelectionListener(selectionListener::accept);

		widgets.setToolbarRenderer(() -> {
			if (ImGui.button("Remove")) onRemoveWidget();
			ImGui.sameLine();
			if (ImGui.button("Modify Value")) onModifyValue();
			ImGui.sameLine();
			if (ImGui.button("Refresh")) { if (refreshHook != null) refreshHook.run(); }
			ImGui.sameLine();
			if (ImGui.button(selectorMode ? "Inspect (ON)" : "Inspect")) {
				selectorMode = !selectorMode;
				System.out.println("GUI Inspector: " + (selectorMode ? "ON" : "OFF"));
			}
			ImGui.sameLine();
			if (ImGui.button(usingYarnMappings ? "Switch to MojMap" : "Switch to Yarn")) onToggleMappings();
		});

		widgets.sideMenuActivationCallback = (entry) -> {
			onModifyValue();
		};

		drawCalls.getRootNode().setUserObject((FriendlyDisplay) () -> "Capture");

		drawCalls.addSelectionListener(e -> {
			drawCalls.getSideMenuEntries().forEach(entry -> {
				if (entry instanceof TextureSnapshot textureSnapshot && textureSnapshot.glId() > 0) {
					glDeleteTextures(textureSnapshot.glId());
				}
			});
			drawCalls.clearSideMenu();
			DefaultMutableTreeNode selectedNode = drawCalls.getSelectedNode();
			if (selectedNode == null || selectedNode.getUserObject() == null) return;
			if (selectedNode.getUserObject() instanceof NodeData nodeData) {
				drawCalls.setSideMenuEntries(List.of(nodeData.details()));
			}
		});

		drawCalls.setToolbarRenderer(() -> {
			if (ImGui.button("Capture")) onCapture();
			ImGui.sameLine();
			ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, new ImVec2(0.0f, 0.0f));
			if (ImGui.button("Detailed names")) onToggleNames();
			ImGui.sameLine();
			if (ImGui.checkbox("##detailednames", useDetailedDrawCallNames)) onToggleNames();
			ImGui.popStyleVar(1);
			ImGui.sameLine();
			if (ImGui.button(drawCallViewIsTree ? "Linear view" : "Tree view")) onToggleView();
			ImGui.sameLine();
			if (ImGui.button("Clear")) onClearDrawCalls();
			ImGui.sameLine();
			if (ImGui.button(usingYarnMappings ? "Switch to MojMap" : "Switch to Yarn")) onToggleMappings();
		});

		treeWindow.show();

		new Thread(() -> {
			Popups.Entry loadingDialog = Popups.dialog("Loading mappings", "Loading mappings, please wait...");

			MappingsResolver.MappingsException error = null;

			try {
				synchronized (mappingLock) {
					mappingsResolver.loadMojMap();
				}
			} catch (MappingsResolver.MappingsException ex) {
				error = ex;
			}

			MappingsResolver.MappingsException finalError = error;

			try {
				treeWindow.getGlWindow().callInContext(() -> {
					loadingDialog.close();

					if (finalError != null) {
						Popups.message("Error", "Failed to load MojMap: " + finalError.getMessage());
					}
					return null;
				});
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				loadingDialog.close();
			}
		}).start();

		// maybe add some sort of key combo for inspector mode later?
//		ClientTickEvents.END_CLIENT_TICK.register(client -> {
//			while (toggleKey.wasPressed()) {
//				selectorMode = !selectorMode;
//				System.out.println("GUI Inspector: " + (selectorMode ? "ON" : "OFF"));
//			}
//		});

		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			treeWindow.start();
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			treeWindow.stop();
		});

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> afterInit(client, screen));
	}

	private void onToggleMappings() {
		usingYarnMappings = !usingYarnMappings;
		treeWindow.getSelectedTab().setSelectedNode(treeWindow.getSelectedTab().getSelectedNode());
	}

	private void onRemoveWidget() {
		DefaultMutableTreeNode selected = widgets.getSelectedNode();
		if (selected == null || selected == widgets.getRootNode()) {
			Popups.message("No Selection", "Select a node to remove.");
			return;
		}
		Popups.confirm("Warning", "This may potentially break things. Are you sure?", confirmed -> {
			if (confirmed && removeHook != null) {
				removeHook.accept(selected.getUserObject());
			}
			widgets.removeNode(selected);
		});
	}

	private void onModifyValue() {
		Object selectedValue = widgets.getSelectedSideMenuEntry();
		if (selectedValue == null) {
			Popups.message("No Selection", "Select an entry in the details pane to modify.");
			return;
		}
		if (!(selectedValue instanceof RowData<?> rd)) {
			Popups.message("Unmodifiable", "Unmodifiable entry.");
			return;
		}

		var handler = modifyValueHandlers.get(rd.type());
		if (handler == null) {
			Popups.message("Error", "Unhandled type: " + getMappedClassName(rd.type()) + ". Please report this to the developer along with the error message.");
			LOGGER.error("Unhandled type: {}. Please report this to the developer along with the error message.", getMappedClassName(rd.type()));
			return;
		}

		DefaultMutableTreeNode nodeAtOpen = widgets.getSelectedNode();
		// TODO: close the edit dialog automatically if a different entry gets selected, or the side menu is cleared
		handler.accept(rd, result -> result.ifPresent(r -> {
			if (widgets.getSelectedNode() != nodeAtOpen) return;
			@SuppressWarnings("unchecked")
			RowData<Object> typedRow = (RowData<Object>) rd;
			typedRow.setter().accept(r);
			selectionListener.accept(null);
		}));
	}

	private void onCapture() {
		Popups.confirm("Confirmation", "This will clear the currently captured draw calls. Are you sure?", confirmed -> {
			if (!confirmed) return;
			System.out.println("Capturing!");
			drawCalls.showSideMenu();
			drawCalls.clear();
			pendingCapture = true;
		});
	}

	private void onToggleNames() {
		if (treeBuilder == null) return;
		useDetailedDrawCallNames = !useDetailedDrawCallNames;
		Enumeration<?> enumeration = drawCalls.getRootNode().depthFirstEnumeration();
		while (enumeration.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
			if (node.getUserObject() instanceof AlternatingDisplayNodeData alternatingDisplayNodeData) {
				alternatingDisplayNodeData.setAltDisplay(!useDetailedDrawCallNames);
			}
		}
	}

	private void onToggleView() {
		if (treeBuilder == null) return;
		drawCalls.clear();
		drawCallViewIsTree = !drawCallViewIsTree;

		if (!drawCallViewIsTree) treeBuilder.buildFlatTree(drawCalls);
		else treeBuilder.buildTree(drawCalls);

		drawCalls.collapseAll();
	}

	private void onClearDrawCalls() {
		Popups.confirm("Confirmation", "This will clear the currently captured draw calls. Are you sure?", confirmed -> {
			if (!confirmed) return;
			drawCalls.hideSideMenu();
			drawCalls.clear();
		});
	}

	public static String getMappedClassName(Object object) {
		return getMappedClassName(object.getClass());
	}

	public static String getMappedClassName(Class<?> clazz) {
		String name = clazz.getName();
		String mappedName = usingYarnMappings ?
				mappingsResolver.getYarnClassName(name) :
				mappingsResolver.getMojMapClassName(name);
		return mappedName == null ? name : mappedName;
	}

	public static String getMappedMethodName(Class<?> clazz, String methodName) {
		String mappedName = usingYarnMappings ?
				mappingsResolver.getYarnMethodName(clazz, methodName) :
				mappingsResolver.getMojMapMethodName(clazz, methodName);
		return mappedName == null ? methodName : mappedName;
	}

	private <T> Consumer<T> warnBreakingSetter(Consumer<T> setter) {
		return object -> Popups.confirm("Warning", "This may potentially break things. Are you sure?", confirmed -> {
			if (confirmed) setter.accept(object);
		});
	}

	private void afterInit(Minecraft client, Screen screen) {

		removeHook = (object) -> {
			if (!(object instanceof GuiEventListener target)) return;

			Consumer<ContainerEventHandler> traverseAndRemove = new Consumer<>() {
				@Override
				public void accept(ContainerEventHandler parent) {
					Iterator<? extends GuiEventListener> it = parent.children().iterator();
					while (it.hasNext()) {
						GuiEventListener child = it.next();

						if (child == target) {
							try {
								it.remove();
							} catch (UnsupportedOperationException e) {
								Popups.message("Cannot Remove", "The element cannot be removed. The containing list is immutable.");
								return;
							}
						} else if (child instanceof ContainerEventHandler pe) {
							accept(pe);
						}
					}
				}
			};

			traverseAndRemove.accept(screen);
		};
		toggleVisibilityHook = (object) -> {
			if (object instanceof AbstractWidget widget) widget.visible = !widget.visible;
		};

		refreshHook = () -> {

			System.out.println("refresh");

			widgets.clear();
			widgets.clearSideMenu();

			if (client.screen != screen) {
				if (client.screen == null) return;

				afterInit(client, client.screen);

				return;
			}

			widgets.getRootNode().setUserObject(screen);

			treeWidgets(screen.children(), widgets.getRootNode());
			if (screen instanceof AbstractContainerScreen<?> handledScreen) {
				for (Slot slot : handledScreen.getMenu().slots) {
					widgets.addNode(slot);
				}
			}
		};

		refreshHook.run();

		ScreenEvents.afterRender(screen).register((scr, context, mouseX, mouseY, tickDelta) -> {

			if (!GUIInspector.selectorMode) return;

			for (Object object : GUIInspector.findAllDeepestWidgetsAt(scr.children(), mouseX, mouseY)) {
				if (object instanceof LayoutElement widget) {
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

	private static void addSideMenuWidthHeightSetterEntry(LayoutElement widget, com.arcadesmasher.guiinspector.WidthHeightRegistry.Enum widthHeight) {
		Consumer<Integer> setter = com.arcadesmasher.guiinspector.WidthHeightRegistry.getSetter(widthHeight, widget);
		widgets.addSideMenuEntry(new DefaultRowData<>(widthHeight == com.arcadesmasher.guiinspector.WidthHeightRegistry.Enum.WIDTH ? widget::getWidth : widget::getHeight, setter, widthHeight.getValue() + ": ", Integer.class));
	}

	public static List<Object> findAllDeepestWidgetsAt(List<?> widgets, double x, double y) {
		List<Object> deepestWidgets = new ArrayList<>();

		for (Object obj : widgets) {
			if (obj instanceof LayoutElement widget && x >= widget.getX() && x <= (widget.getX() + widget.getWidth())
											&&	y >= widget.getY() && y <= (widget.getY() + widget.getHeight())) {
				if (widget instanceof ContainerEventHandler parent) {
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
					if (obj instanceof ContainerEventHandler parent) {
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
			if (object instanceof ContainerEventHandler parentElement) {
				treeWidgets(parentElement.children(), child);
			}
		}
	}

	private static void logWidgets(List<?> objects) {
		logWidgets(objects, 0);
	}

	private static void logWidgets(List<?> objects, int depth) {

		for (Object object : objects) {
			System.out.println("  ".repeat(depth) + getMappedClassName(object));
			if (object instanceof LayoutElement widget) {
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
			if (object instanceof ContainerEventHandler parentElement) {
				System.out.println("  ".repeat(depth) + "CHILDREN:");
				logWidgets(parentElement.children(), depth + 1);
			}
			System.out.println("\n");
		}
	}

	public static void drawOutline(GuiGraphics context, int x, int y, int width, int height) {

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



	public static List<StackWalker.StackFrame> getFrames(int skip) {
		return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.walk(s -> s.skip(1 + skip).toList());
	}

	public static Object[] buildGenericFrameDetails(StackWalker.StackFrame frame) {
		return buildGenericFrameDetails(frame, null);
	}

	public static Object[] buildGenericFrameDetails(StackWalker.StackFrame frame, String label) {
		return new Object[]{
				"Full method: " +	(label != null ? label : buildFrameLabel(frame)),
				"Class: " +			GUIInspector.getMappedClassName(frame.getDeclaringClass()),
				"Method: " +		GUIInspector.getMappedMethodName(frame.getDeclaringClass(), frame.getMethodName()),
				"File: " +			frame.getFileName(),
				"Line: " +			frame.getLineNumber(),
				"Native: " +		frame.isNativeMethod(),
				"Module: " +		(frame.getDeclaringClass().getModule() != null ? frame.getDeclaringClass().getModule().getName() : "unknown"),
				"Package: " +		frame.getClassName().substring(0, Math.max(frame.getClassName().lastIndexOf('.'), 0))
		};
	}

	public static Object[] combineDetails(Object[] first, Object[] second) {
		Object[] combined = new Object[first.length + second.length];
		System.arraycopy(first, 0, combined, 0, first.length);
		System.arraycopy(second, 0, combined, first.length, second.length);
		return combined;
	}

	public static String buildFrameLabel(StackWalker.StackFrame frame) {
		StringBuilder sb = new StringBuilder();

		sb.append(GUIInspector.getMappedClassName(frame.getDeclaringClass()))
				.append('@')
				.append(Integer.toHexString(frame.getDeclaringClass().hashCode()))
				.append("    ")
				.append(GUIInspector.getMappedClassName(frame.getMethodType().returnType()))
				.append(' ')
				.append(GUIInspector.getMappedMethodName(frame.getDeclaringClass(), frame.getMethodName()))
				.append('(');

		boolean first = true;
		for (var param : frame.getMethodType().parameterList()) {
			if (!first) sb.append(", ");
			sb.append(GUIInspector.getMappedClassName(param));
			first = false;
		}

		sb.append(')');

		return sb.toString();
	}

	public static String buildFrameLabelNoMap(StackWalker.StackFrame frame) {
		StringBuilder sb = new StringBuilder();

		sb.append(frame.getDeclaringClass())
				.append('@')
				.append(Integer.toHexString(frame.getDeclaringClass().hashCode()))
				.append("    ")
				.append(frame.getMethodType().returnType())
				.append(' ')
				.append(frame.getDeclaringClass())
				.append('(');

		boolean first = true;
		for (var param : frame.getMethodType().parameterList()) {
			if (!first) sb.append(", ");
			sb.append(param);
			first = false;
		}

		sb.append(')');

		return sb.toString();
	}

	public static String buildSimpleFrameLabel(StackWalker.StackFrame frame) {
		return GUIInspector.getMappedMethodName(frame.getDeclaringClass(), frame.getMethodName()) + "(...)";
	}

	// returns [a, r, g, b]
	public static int[] decodeColor(int color) {
		return new int[]{
				(color >> 24) & 0xFF,
				(color >> 16) & 0xFF,
				(color >> 8)  & 0xFF,
				color & 0xFF
		};
	}

	public static void MouseMixin_onMouseButton(Object self, long window, int button, int action, CallbackInfo ci) {
		MouseMixin_onMouseButton(self, window, button, action, ci, true);
	}

	public static void MouseMixin_onMouseButton(Object self, long window, int button, int action, CallbackInfo ci, boolean shouldRetry) {

		if (!selectorMode) return;
		if (button != 0) return;
		if (action != 1) return;

		MouseHandler mouse = (MouseHandler) self;

		if (mouse.minecraft.screen == null) return;
		com.mojang.blaze3d.platform.Window mouseWindow = mouse.minecraft.getWindow();
		//#if _1_20_1
		//$$ if (mouseWindow.getWindow() != window) return;
		//#elif _1_21_11
		if (mouseWindow.handle() != window) return;
		//#endif

		activeWidgets.clear();
		activeWidgets.ensureCapacity(mouse.minecraft.screen.children().size());

		activeWidgets.addAll(findAllDeepestWidgetsAt(mouse.minecraft.screen.children(), mouse.xpos() * (double)mouseWindow.getGuiScaledWidth() / (double)mouseWindow.getScreenWidth(), mouse.ypos() * (double)mouseWindow.getGuiScaledHeight() / (double)mouseWindow.getScreenHeight()));

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
					if (shouldRetry) {
						MouseMixin_onMouseButton(self, window, button, action, ci, false);
					}
				}
				widgets.setSelectedNode(selectedWidget, s -> {
					if (!s) {
						System.out.println(getMappedClassName(mouse.minecraft.screen));
//								logWidgets(scr.children());
					}
				});
			});
		}
		activeWidgets.clear();
		ci.cancel();
	}
}