package com.arcadesmasher.guiinspector.data.nodedata;

public abstract class AlternatingDisplayNodeData extends AbstractNodeData {

	private final String altDisplay;
	private boolean isAltDisplay = false;

	protected AlternatingDisplayNodeData(String display, String altDisplay, Object[] details) {
		super(display, details);
		this.altDisplay = altDisplay;
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
		return isAltDisplay ? altDisplay : super.display();
	}
}