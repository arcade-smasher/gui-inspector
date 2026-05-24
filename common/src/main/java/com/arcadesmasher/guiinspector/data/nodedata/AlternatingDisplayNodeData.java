package com.arcadesmasher.guiinspector.data.nodedata;

import com.arcadesmasher.guiinspector.GUIInspector;

public abstract class AlternatingDisplayNodeData extends AbstractNodeData {

	private final String yarnAltDisplay;
	private final String mojmapAltDisplay;
	private boolean isAltDisplay = false;

	protected AlternatingDisplayNodeData(String yarnDisplay, String yarnAltDisplay, String mojmapDisplay, String mojmapAltDisplay, Object[] yarnDetails, Object[] mojmapDetails) {
		super(yarnDisplay, mojmapDisplay, yarnDetails, mojmapDetails);
		this.yarnAltDisplay = yarnAltDisplay;
		this.mojmapAltDisplay = mojmapAltDisplay;
	}

	/**
	 * @return Whether the display is now the alt display.
	 */
	public boolean swapDisplay() {
		isAltDisplay = !isAltDisplay;
		return isAltDisplay;
	}

	public boolean isAltDisplay() {
		return isAltDisplay;
	}

	public void setAltDisplay(boolean altDisplay) {
		isAltDisplay = altDisplay;
	}

	@Override
	public String display() {
		return isAltDisplay ? (GUIInspector.usingYarnMappings ? yarnAltDisplay : mojmapAltDisplay) : super.display();
	}
}