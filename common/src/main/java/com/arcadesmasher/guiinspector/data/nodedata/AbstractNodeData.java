package com.arcadesmasher.guiinspector.data.nodedata;

import com.arcadesmasher.guiinspector.GUIInspector;
import com.arcadesmasher.guiinspector.data.AbstractFriendlyDisplay;

public abstract class AbstractNodeData extends AbstractFriendlyDisplay implements com.arcadesmasher.guiinspector.data.nodedata.NodeData {

	private final String yarnDisplay;
	private final String mojmapDisplay;
	private final Object[] yarnDetails;
	private final Object[] mojmapDetails;

	protected AbstractNodeData(String yarnDisplay, String mojmapDisplay, Object[] yarnDetails, Object[] mojmapDetails) {
		super(yarnDisplay);
		this.yarnDisplay = yarnDisplay;
		this.mojmapDisplay = mojmapDisplay;
		this.yarnDetails = yarnDetails;
		this.mojmapDetails = mojmapDetails;
	}

	public Object[] yarnDetails() {
		return yarnDetails;
	}
	public Object[] mojmapDetails() {
		return mojmapDetails;
	}
	@Override
	public Object[] details() {
		return GUIInspector.usingYarnMappings ? yarnDetails : mojmapDetails;
	}
	@Override
	public String display() {
		return GUIInspector.usingYarnMappings ? yarnDisplay : mojmapDisplay;
	}
}
