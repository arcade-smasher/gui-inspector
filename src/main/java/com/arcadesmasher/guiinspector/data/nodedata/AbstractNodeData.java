package com.arcadesmasher.guiinspector.data.nodedata;

import com.arcadesmasher.guiinspector.data.AbstractFriendlyDisplay;

public abstract class AbstractNodeData extends AbstractFriendlyDisplay implements NodeData {

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
