package com.arcadesmasher.guiinspector;

import com.arcadesmasher.guiinspector.data.nodedata.DrawCallDataTreeBuilder;
import com.arcadesmasher.guiinspector.tree.TreePanel;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;

public class TreeBuilder {
	private final List<DrawCallDataTreeBuilder[]> chains = new ArrayList<>();

	public void collectChain(DrawCallDataTreeBuilder[] chain) {
		if (chain == null || chain.length == 0) return; // ignore empty
		this.chains.add(chain);
	}

	private static class MergeNode {
		final String id;
		final Object userObject;

		final LinkedHashMap<String, MergeNode> children = new LinkedHashMap<>();

		final ArrayList<Object> leaves = new ArrayList<>();

		MergeNode(String id, Object userObject) {
			this.id = id;
			this.userObject = userObject;
		}
	}

	public void buildTree(TreePanel panel) {
		MergeNode root = new MergeNode(null, null);

		for (DrawCallDataTreeBuilder[] chain : chains) {
			if (chain.length == 0) continue;
			MergeNode cur = root;
			for (int i = chain.length - 1; i >= 1; i--) {
				DrawCallDataTreeBuilder nodeData = chain[i];
				String key = nodeData.id();
				cur = cur.children.computeIfAbsent(key, k -> new MergeNode(k, nodeData.data()));
			}
			// now cur is the parent under which the leaf(s) are appended. do NOT dedupe leaves; add each leaf as separate entry
			DrawCallDataTreeBuilder leaf = chain[0];
			cur.leaves.add(leaf.data());
		}

		for (Object leafObj : root.leaves) {
			panel.addChildNode(null, leafObj);
		}

		for (MergeNode child : root.children.values()) {
			createNodesRecursively(panel, null, child);
		}
	}

	private void createNodesRecursively(TreePanel panel, DefaultMutableTreeNode parentNode, MergeNode node) {
		DefaultMutableTreeNode created = panel.addChildNode(parentNode, node.userObject);

		for (Object leafObj : node.leaves) {
			panel.addChildNode(created, leafObj);
		}

		for (MergeNode child : node.children.values()) {
			createNodesRecursively(panel, created, child);
		}
	}

	public void buildFlatTree(TreePanel panel) {
		chains.forEach(chain -> {
			DefaultMutableTreeNode parentNode = panel.getRootNode();
			for (DrawCallDataTreeBuilder frame : chain) {
				parentNode = panel.addChildNode(parentNode, frame.data());
			}
		});
	}
}
