package com.arcadesmasher.guiinspector.data.nodedata;

import com.arcadesmasher.guiinspector.GUIInspector;
import com.arcadesmasher.guiinspector.data.AbstractFriendlyDisplay;

public abstract class AbstractNodeData extends AbstractFriendlyDisplay implements com.arcadesmasher.guiinspector.data.nodedata.NodeData {

	private final Object[] details;

	protected AbstractNodeData(String display, Object[] details) {
		super(display);
		this.details = details;
	}

	@Override
	public Object[] details() {
		return details;
	}
}
