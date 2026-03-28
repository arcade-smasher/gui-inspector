package com.arcadesmasher.guiinspector;

import net.minecraft.client.gui.widget.ClickableWidget;

public class VersionBridge {

	public static void addSideMenuEntry_ClickableWidget_Interactable(ClickableWidget clickableWidget) {
		GUIInspector.widgets.addSideMenuEntry("Narratable: " + clickableWidget.isNarratable());
	}
}
