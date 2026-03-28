package me.jellysquid.mods.sodium.client.gui.options.control;

import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;

// not abstract
public abstract class ControlElement<T> extends AbstractWidget {

	public abstract Dim2i getDimensions(); // not abstract
}
