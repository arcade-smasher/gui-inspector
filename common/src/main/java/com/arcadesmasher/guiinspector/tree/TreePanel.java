package com.arcadesmasher.guiinspector.tree;

import com.arcadesmasher.guiinspector.GUIInspector;
import com.arcadesmasher.guiinspector.data.FriendlyDisplay;
import com.arcadesmasher.guiinspector.data.nodedata.TextureSnapshot;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.flag.ImGuiWindowFlags;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class TreePanel {

	private enum FocusArea {
		TREE,
		DETAILS
	}

	private FocusArea focusArea = FocusArea.TREE;

	private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Root");

	private final Queue<Runnable> pendingMutations = new ConcurrentLinkedQueue<>();

	private final List<TreeSelectionListener> selectionListeners = new ArrayList<>();
	private volatile DefaultMutableTreeNode selectedNode;

	private final Set<DefaultMutableTreeNode> expandedNodes = Collections.newSetFromMap(new ConcurrentHashMap<>());

	private final List<Object> sideMenuEntries = new ArrayList<>();
	private volatile boolean sideMenuVisible = true;
	private int sideMenuSelectedIndex = -1;

	private float sideMenuWidth = 220f;
	private static final float MIN_TREE_WIDTH = 100f;
	private static final float MIN_SIDE_WIDTH = 150f;
	private static final float SPLITTER_WIDTH = 6f;

	private volatile Runnable toolbarRenderer;

	public Consumer<Object> sideMenuActivationCallback;

	public TreePanel() {}

	public void addSelectionListener(TreeSelectionListener listener) {
		selectionListeners.add(listener);
	}

	public DefaultMutableTreeNode addNode(Object userObject) {
		return addChildNode(null, userObject);
	}

	public DefaultMutableTreeNode addChildNode(DefaultMutableTreeNode parent, Object userObject) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(userObject);
		DefaultMutableTreeNode actualParent = parent == null ? rootNode : parent;
		pendingMutations.add(() -> {
			actualParent.add(node);
			expandedNodes.add(actualParent);
		});
		return node;
	}

	public void removeNode(DefaultMutableTreeNode node) {
		if (node == rootNode) return;
		pendingMutations.add(node::removeFromParent);
	}

	public void clear() {
		pendingMutations.add(() -> {
			rootNode.removeAllChildren();
			expandedNodes.clear();
			selectedNode = null;
		});
	}

	public boolean isNodeVisible(DefaultMutableTreeNode node) {
		if (node == null) return false;
		if (node.getPreviousNode() == null) return true; // has to be visible; it's the root
		return isNodeExpanded(node.getPreviousNode());
	}

	public boolean isNodeExpanded(DefaultMutableTreeNode node) {
		return expandedNodes.contains(node);
	}

	public DefaultMutableTreeNode findNode(Object target) {
		return findNode(rootNode, target);
	}

	public static DefaultMutableTreeNode findNode(DefaultMutableTreeNode node, Object target) {
		if (node.getUserObject() != null && node.getUserObject().equals(target)) {
			return node;
		}
		for (int i = 0; i < node.getChildCount(); i++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
			DefaultMutableTreeNode result = findNode(child, target);
			if (result != null) return result;
		}
		return null;
	}

	public DefaultMutableTreeNode getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(DefaultMutableTreeNode node) {
		setSelectedNode(node, null);
	}

	public void setSelectedNode(DefaultMutableTreeNode node, Consumer<Boolean> callback) {
		pendingMutations.add(() -> {
			DefaultMutableTreeNode old = selectedNode;
			selectedNode = node;
			if (node != null) {
				DefaultMutableTreeNode p = (DefaultMutableTreeNode) node.getParent();
				while (p != null) {
					expandedNodes.add(p);
					p = (DefaultMutableTreeNode) p.getParent();
				}
			}
			fireSelectionChanged(old, node);
			if (callback != null) callback.accept(true);
		});
	}

	public void expandNode(DefaultMutableTreeNode node) {
		pendingMutations.add(() -> {
			expandedNodes.add(node);
		});
	}

	public void showSideMenu() {
		sideMenuVisible = true;
	}

	public void hideSideMenu() {
		sideMenuVisible = false;
	}

	public void toggleSideMenu() {
		sideMenuVisible = !sideMenuVisible;
	}

	public void clearSideMenu() {
		pendingMutations.add(sideMenuEntries::clear);
	}

	public void addSideMenuEntry(Object entry) {
		pendingMutations.add(() -> sideMenuEntries.add(entry));
	}

	public void setSideMenuEntries(List<Object> entries) {
		pendingMutations.add(() -> {
			sideMenuEntries.clear();
			sideMenuEntries.addAll(entries);
		});
	}

	public List<Object> getSideMenuEntries() {
		return sideMenuEntries;
	}

	public void setToolbarRenderer(Runnable renderer) {
		this.toolbarRenderer = renderer;
	}

	public DefaultMutableTreeNode getRootNode() {
		return rootNode;
	}

	public Object getSelectedSideMenuEntry() {
		if (sideMenuSelectedIndex < 0 || sideMenuSelectedIndex >= sideMenuEntries.size()) return null;
		return sideMenuEntries.get(sideMenuSelectedIndex);
	}

	public void collapseAll() {
		pendingMutations.add(expandedNodes::clear);
	}

	void render() {
		Runnable r;
		while ((r = pendingMutations.poll()) != null) {
			r.run();
		}

		if (toolbarRenderer != null) {
			toolbarRenderer.run();
			ImGui.separator();
		}

		if (!sideMenuVisible) {
			ImGui.beginChild("##tree_area", 0, 0, false, ImGuiWindowFlags.HorizontalScrollbar);

			renderNode(rootNode);

			ImGui.endChild();
			return;
		}

		float availableWidth = ImGui.getContentRegionAvailX();

		float maxSideWidth = availableWidth - MIN_TREE_WIDTH - SPLITTER_WIDTH;
		sideMenuWidth = Math.max(MIN_SIDE_WIDTH, Math.min(sideMenuWidth, maxSideWidth));

		float treeWidth = availableWidth - sideMenuWidth - SPLITTER_WIDTH;

		ImGui.beginChild("##tree_area", treeWidth, 0, false, ImGuiWindowFlags.HorizontalScrollbar);

		if (ImGui.isWindowHovered() && ImGui.isMouseClicked(0)) focusArea = FocusArea.TREE;

		// starting to regret switching to imgui
		if (focusArea == FocusArea.TREE && selectedNode != null) {
			if (ImGui.isKeyPressed(ImGuiKey.UpArrow)) {
				DefaultMutableTreeNode current = selectedNode;
				if (current != null) {
					DefaultMutableTreeNode prevSibling = current.getPreviousSibling();
					if (prevSibling != null) {
						DefaultMutableTreeNode target = prevSibling;
						while (isNodeExpanded(target) && target.getChildCount() > 0) {
							target = (DefaultMutableTreeNode) target.getLastChild();
						}
						setSelectedNode(target);
					} else {
						DefaultMutableTreeNode parent = (DefaultMutableTreeNode) current.getParent();
						if (parent != null) {
							setSelectedNode(parent);
						}
					}
				}
			} else if (ImGui.isKeyPressed(ImGuiKey.DownArrow)) {
				DefaultMutableTreeNode current = selectedNode;
				if (current != null) {
					DefaultMutableTreeNode child = current.getNextNode();
					if (child != null && isNodeExpanded(current)) {
						setSelectedNode(child);
					} else {
						DefaultMutableTreeNode node = current;
						DefaultMutableTreeNode target = null;
						while (node != null) {
							DefaultMutableTreeNode sibling = node.getNextSibling();
							if (sibling != null) {
								target = sibling;
								break;
							}
							node = (DefaultMutableTreeNode) node.getParent();
						}
						if (target != null) {
							setSelectedNode(target);
						}
					}
				}
			}
		}

		renderNode(rootNode);

		ImGui.endChild();

		ImGui.sameLine(0, 0);

		ImGui.invisibleButton("##splitter", SPLITTER_WIDTH, ImGui.getContentRegionAvailY());

		if (ImGui.isItemActive()) {
			sideMenuWidth -= ImGui.getIO().getMouseDeltaX();
		}

		if (ImGui.isItemHovered() || ImGui.isItemActive()) {
			ImGui.setMouseCursor(imgui.flag.ImGuiMouseCursor.ResizeEW);
		}

		ImGui.sameLine(0, 0);

		ImGui.beginChild("##side_menu", 0, 0, true, ImGuiWindowFlags.HorizontalScrollbar);

		if (ImGui.isWindowHovered() && ImGui.isMouseClicked(0)) focusArea = FocusArea.DETAILS;

		if (focusArea == FocusArea.DETAILS && !sideMenuEntries.isEmpty()) {
			if (ImGui.isKeyPressed(ImGuiKey.UpArrow)) {
				if (sideMenuSelectedIndex <= 0) {
					sideMenuSelectedIndex = sideMenuEntries.size() - 1;
				} else {
					sideMenuSelectedIndex--;
				}
			}

			if (ImGui.isKeyPressed(ImGuiKey.DownArrow)) {
				if (sideMenuSelectedIndex < 0 || sideMenuSelectedIndex >= sideMenuEntries.size() - 1) {
					sideMenuSelectedIndex = 0;
				} else {
					sideMenuSelectedIndex++;
				}
			}
		}

		if (focusArea == FocusArea.DETAILS && sideMenuSelectedIndex >= 0 && ImGui.isKeyPressed(ImGuiKey.Enter)) {
			activateSideMenuEntry(sideMenuSelectedIndex);
		}

		ImGui.text("Details");
		ImGui.separator();

		for (int i = 0; i < sideMenuEntries.size(); i++) {
			renderSideMenuEntry(i, sideMenuEntries.get(i));
		}

		ImGui.endChild();
	}

	private void renderSideMenuEntry(int index, Object entry) {
		String popupId = "##side_ctx" + index;

		if (entry instanceof TextureSnapshot image) {
			if (image != TextureSnapshot.EMPTY) {
				ImGui.image(
						image.glId(),
						image.width(),
						image.height()
				);
				return;
			} else {
				entry = (FriendlyDisplay) () -> "[Could not display image]";
			}
		}

		String display = entry instanceof FriendlyDisplay fd
				? fd.display()
				: String.valueOf(entry);

		if (ImGui.selectable(
				display + "##side" + index,
				sideMenuSelectedIndex == index
		)) {
			sideMenuSelectedIndex = index;
		}

		if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0)) {
			activateSideMenuEntry(sideMenuSelectedIndex);
		}

		if (ImGui.beginPopupContextItem(popupId)) {
			if (ImGui.menuItem("Copy")) {
				ImGui.setClipboardText(display);
			}
			ImGui.endPopup();
		}
	}

	private void activateSideMenuEntry(int index) {
		if (index < 0) return;

		sideMenuSelectedIndex = index;

		if (sideMenuActivationCallback != null) {
			sideMenuActivationCallback.accept(sideMenuEntries.get(index));
		}
	}

	private void renderNode(DefaultMutableTreeNode node) {
		Object userObject = node.getUserObject();
		String label = userObject instanceof FriendlyDisplay fd
				? fd.display()
				: String.valueOf(GUIInspector.getMappedClassName(userObject));

		boolean isLeaf = node.getChildCount() == 0;
		int flags = ImGuiTreeNodeFlags.OpenOnArrow | ImGuiTreeNodeFlags.OpenOnDoubleClick | ImGuiTreeNodeFlags.SpanAvailWidth;
		if (isLeaf) flags |= ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.NoTreePushOnOpen;
		if (node == selectedNode) flags |= ImGuiTreeNodeFlags.Selected;
		if (expandedNodes.contains(node)) ImGui.setNextItemOpen(true, ImGuiCond.Once);

		boolean isSelectedTreeNode = focusArea == FocusArea.TREE && node == selectedNode;

		if (isSelectedTreeNode) {
			boolean currentlyExpanded = expandedNodes.contains(node);
			if (ImGui.isKeyPressed(ImGuiKey.Enter)) {
				ImGui.setNextItemOpen(!currentlyExpanded, ImGuiCond.Always);
			} else if (ImGui.isKeyPressed(ImGuiKey.RightArrow)) {
				if (!currentlyExpanded) {
					ImGui.setNextItemOpen(true, ImGuiCond.Always);
				} else {
					if (node.getNextNode() != null) {
						setSelectedNode(node.getNextNode());
					}
				}
			} else if (ImGui.isKeyPressed(ImGuiKey.LeftArrow)) {
				if (currentlyExpanded) {
					ImGui.setNextItemOpen(false, ImGuiCond.Always);
				} else {
					if (node.getPreviousNode() != null) {
						setSelectedNode(node.getPreviousNode());
					}
				}
			}
		}

		boolean open = ImGui.treeNodeEx(Long.toString(System.identityHashCode(node)), flags, label);

		if (ImGui.isItemClicked()) {
			DefaultMutableTreeNode old = selectedNode;
			selectedNode = node;
			fireSelectionChanged(old, node);
		}

		if (!isLeaf) {
			if (open) {
				expandedNodes.add(node);
				for (int i = 0; i < node.getChildCount(); i++) {
					renderNode((DefaultMutableTreeNode) node.getChildAt(i));
				}
				ImGui.treePop();
			} else {
				expandedNodes.remove(node);
			}
		}
	}

	private void fireSelectionChanged(DefaultMutableTreeNode oldNode, DefaultMutableTreeNode newNode) {
		if (selectionListeners.isEmpty()) return;
		TreePath oldPath = oldNode == null ? null : new TreePath(oldNode.getPath());
		TreePath newPath = newNode == null ? null : new TreePath(newNode.getPath());
		TreeSelectionEvent event = new TreeSelectionEvent(this, newPath, true, oldPath, newPath);
		for (TreeSelectionListener l : selectionListeners) {
			l.valueChanged(event);
		}
	}
}