package com.arcadesmasher.guiinspector.tree;

import com.arcadesmasher.guiinspector.data.FriendlyDisplay;
import com.arcadesmasher.guiinspector.mappings.ClassMappings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

// everything should be thread-safe... as well as TreeWindow
public class TreePanel extends JPanel {

	private final DefaultMutableTreeNode rootNode;
	private final DefaultTreeModel treeModel;
	private final JTree tree;

	public JPanel toolbar;

	private final JPanel sideMenu;
	private final DefaultListModel<Object> sideMenuModel;
	private final JList<Object> sideMenuList;
	private final JSplitPane splitPane;

	public TreePanel() {
		super(new BorderLayout());

		rootNode = new DefaultMutableTreeNode("Root");
		treeModel = new DefaultTreeModel(rootNode);
		tree = new JTree(treeModel);
		tree.setRootVisible(true);
		tree.setShowsRootHandles(true);
		tree.setCellRenderer(new DefaultTreeCellRenderer() {
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
														  boolean expanded, boolean leaf, int row, boolean hasFocus) {
				if (value instanceof DefaultMutableTreeNode node) {
					if (node.getUserObject() instanceof FriendlyDisplay friendlyDisplay) {
						value = friendlyDisplay.display();
					} else {
						value = ClassMappings.getMappedName(node.getUserObject());
					}
				}
				return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
			}
		});

		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		JScrollPane scrollPane = new JScrollPane(tree);

		toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

		sideMenuModel = new DefaultListModel<>();
		sideMenuList = new JList<>(sideMenuModel);
		sideMenuList.setTransferHandler(new TransferHandler() {
			@Override
			protected Transferable createTransferable(JComponent c) {
				JList<?> list = (JList<?>) c;
				Object selectedValue = list.getSelectedValue();

				if (selectedValue instanceof BufferedImage image) {
					return new Transferable() {
						@Override
						public DataFlavor[] getTransferDataFlavors() {
							return new DataFlavor[]{DataFlavor.imageFlavor};
						}

						@Override
						public boolean isDataFlavorSupported(DataFlavor flavor) {
							return DataFlavor.imageFlavor.equals(flavor);
						}

						@Override
						public @NotNull Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
							if (!isDataFlavorSupported(flavor)) {
								throw new UnsupportedFlavorException(flavor);
							}
							return image;
						}
					};
				} else if (selectedValue instanceof FriendlyDisplay friendlyDisplay) {
					return new StringSelection(friendlyDisplay.display());
				} else {
					return new StringSelection(selectedValue.toString());
				}
			}

			@Override
			public int getSourceActions(JComponent c) {
				return COPY;
			}
		});
		InputMap im = sideMenuList.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap am = sideMenuList.getActionMap();
		im.put(KeyStroke.getKeyStroke("ctrl C"), "copy");
		am.put("copy", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TransferHandler th = sideMenuList.getTransferHandler();
				th.exportToClipboard(sideMenuList, Toolkit.getDefaultToolkit().getSystemClipboard(), TransferHandler.COPY);
			}
		});
		sideMenuList.setFixedCellHeight(-1);
		sideMenuList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof BufferedImage bufferedImage) {
					setText(null);
					setIcon(new ImageIcon(bufferedImage));
				} else if (value instanceof FriendlyDisplay friendlyDisplay) {
					setText(friendlyDisplay.display());
					setIcon(null);
				} else {
					setText(value.toString());
					setIcon(null);
				}
				return this;
			}
		});
		sideMenu = new JPanel(new BorderLayout());
		sideMenu.add(new JLabel("Details"), BorderLayout.NORTH);
		sideMenu.add(new JScrollPane(sideMenuList), BorderLayout.CENTER);
		sideMenu.setPreferredSize(new Dimension(200, 0));

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, sideMenu);
		splitPane.setResizeWeight(1.0); // tree takes remaining space
		splitPane.setDividerLocation(250);

		add(toolbar, BorderLayout.NORTH);
		add(splitPane, BorderLayout.CENTER);
	}

	public void addSelectionListener(TreeSelectionListener listener) {
		tree.addTreeSelectionListener(listener);
	}

	public DefaultMutableTreeNode addNode(Object userObject) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(userObject);
		SwingUtilities.invokeLater(() -> {
			treeModel.insertNodeInto(node, rootNode, rootNode.getChildCount());
			expandAll();
		});
		return node;
	}

	public DefaultMutableTreeNode addChildNode(DefaultMutableTreeNode parent, Object userObject) {
		if (parent == null) return addNode(userObject);
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(userObject);
		SwingUtilities.invokeLater(() -> {
			treeModel.insertNodeInto(node, parent, parent.getChildCount());
			tree.expandPath(new TreePath(treeModel.getPathToRoot(parent)));
		});
		return node;
	}

	public void removeNode(DefaultMutableTreeNode node) {
		if (node == rootNode) return;
		SwingUtilities.invokeLater(() -> treeModel.removeNodeFromParent(node));
	}

	public void clear() {
		SwingUtilities.invokeLater(() -> {
			rootNode.removeAllChildren();
			treeModel.reload();
		});
	}

	public DefaultMutableTreeNode findNode(Object target) {
		return findNode(rootNode, target);
	}

	public static DefaultMutableTreeNode findNode(DefaultMutableTreeNode node, Object target) {
		if (node.getUserObject().equals(target)) {
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
		TreePath path = tree.getSelectionPath();
		if (path == null) return null;
		return (DefaultMutableTreeNode) path.getLastPathComponent();
	}

	public void setSelectedNode(DefaultMutableTreeNode node) {
		setSelectedNode(node, null);
	}

	public void setSelectedNode(DefaultMutableTreeNode node, Consumer<Boolean> callback) {
		SwingUtilities.invokeLater(() -> {
			if (node == null) {
				tree.clearSelection();
				if (callback != null) callback.accept(false);
				return;
			}

			TreePath path = new TreePath(treeModel.getPathToRoot(node));
			tree.setSelectionPath(path);
			tree.scrollPathToVisible(path);
			boolean success = tree.getSelectionPath() != null && tree.getSelectionPath().equals(path);
			if (callback != null) callback.accept(success);
		});
	}

	public void showSideMenu() {
		SwingUtilities.invokeLater(() -> sideMenu.setVisible(true));
	}

	public void hideSideMenu() {
		SwingUtilities.invokeLater(() -> sideMenu.setVisible(false));
	}

	public void toggleSideMenu() {
		SwingUtilities.invokeLater(() -> sideMenu.setVisible(!sideMenu.isVisible()));
	}

	public void clearSideMenu() {
		SwingUtilities.invokeLater(sideMenuModel::clear);
	}

	public void addSideMenuEntry(Object entry) {
		SwingUtilities.invokeLater(() -> sideMenuModel.addElement(entry));
	}

	public void setSideMenuEntries(java.util.List<Object> entries) {
		SwingUtilities.invokeLater(() -> {
			sideMenuModel.clear();
			for (Object entry : entries) {
				sideMenuModel.addElement(entry);
			}
		});
	}


	private void expandAll() {
		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}
	}

	public JPanel getToolbar() { return toolbar; }
	public JList<Object> getSideMenuList() { return sideMenuList; }
	public JTree getTree() { return tree; }
	public DefaultTreeModel getTreeModel() { return treeModel; }
	public DefaultMutableTreeNode getRootNode() { return rootNode; }
}