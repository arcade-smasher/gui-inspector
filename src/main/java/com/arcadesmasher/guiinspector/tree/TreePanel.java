package com.arcadesmasher.guiinspector.tree;

import com.arcadesmasher.guiinspector.data.FriendlyDisplay;
import com.arcadesmasher.guiinspector.mappings.ClassMappings;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.util.function.Consumer;

// everything should be thread-safe... as well as TreeWindow
public class TreePanel extends JPanel {

    private final DefaultMutableTreeNode rootNode;
    private final DefaultTreeModel treeModel;
    private final JTree tree;

    public JPanel toolbar;

    private final JPanel sideMenu;
    private final DefaultListModel<String> sideMenuModel;
    private final JList<String> sideMenuList;
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

    public void addSideMenuEntry(String entry) {
        SwingUtilities.invokeLater(() -> sideMenuModel.addElement(entry));
    }

    public void setSideMenuEntries(java.util.List<String> entries) {
        SwingUtilities.invokeLater(() -> {
            sideMenuModel.clear();
            for (String entry : entries) {
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
    public JTree getTree() { return tree; }
    public DefaultTreeModel getTreeModel() { return treeModel; }
    public DefaultMutableTreeNode getRootNode() { return rootNode; }
}