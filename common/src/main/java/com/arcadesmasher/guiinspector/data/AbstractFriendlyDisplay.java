package com.arcadesmasher.guiinspector.data;

public class AbstractFriendlyDisplay implements FriendlyDisplay {

	private final String display;

	protected AbstractFriendlyDisplay(String display) {
		this.display = display;
	}

	@Override
	public String display() {
		return display;
	}
}
