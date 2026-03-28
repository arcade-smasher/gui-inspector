package com.arcadesmasher.guiinspector;

import com.arcadesmasher.guiinspector.data.rowdata.DefaultRowData;
import com.arcadesmasher.guiinspector.mixin.oldsodium.FlatButtonWidgetAccessor;
import com.arcadesmasher.guiinspector.tree.TreePanel;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.text.Text;

public class SodiumCompatOld implements SodiumCompat {

	@Override
	public void addSodiumEntries(TreePanel widgets, Object object) {
		if (object instanceof ControlElement<?> controlElement) {
			Dim2iAccessor dim2iAccessor = (Dim2iAccessor) (Object) controlElement.getDimensions();
			widgets.addSideMenuEntry("");
			assert dim2iAccessor != null;
			widgets.addSideMenuEntry(new DefaultRowData<>(dim2iAccessor::getX, dim2iAccessor::setX, "x: ", Integer.class));
			widgets.addSideMenuEntry(new DefaultRowData<>(dim2iAccessor::getY, dim2iAccessor::setY, "y: ", Integer.class));
			widgets.addSideMenuEntry(new DefaultRowData<>(dim2iAccessor::getWidth, dim2iAccessor::setWidth, "width: ", Integer.class));
			widgets.addSideMenuEntry(new DefaultRowData<>(dim2iAccessor::getHeight, dim2iAccessor::setHeight, "height: ", Integer.class));
		}
		if (object instanceof AbstractWidgetAccessor abstractWidgetAccessor) {
			widgets.addSideMenuEntry("");
			widgets.addSideMenuEntry(new DefaultRowData<>(abstractWidgetAccessor::getHovered, abstractWidgetAccessor::setHovered, "Hovered: ", Boolean.class));
		}
		if (object instanceof FlatButtonWidgetAccessor flatButtonWidgetAccessor) {
			widgets.addSideMenuEntry("");
			widgets.addSideMenuEntry(new DefaultRowData<>(flatButtonWidgetAccessor::getVisible, flatButtonWidgetAccessor::invokeSetVisible, "Visible: ", Boolean.class));
			widgets.addSideMenuEntry(new DefaultRowData<>(flatButtonWidgetAccessor::isSelected, flatButtonWidgetAccessor::invokeSetSelected, "Selected: ", Boolean.class));
			widgets.addSideMenuEntry(new DefaultRowData<>(flatButtonWidgetAccessor::isEnabled, flatButtonWidgetAccessor::invokeSetEnabled, "Enabled: ", Boolean.class));
			Text label = flatButtonWidgetAccessor.acquireLabel();
			if (label != null) {
				widgets.addSideMenuEntry(new DefaultRowData<>(flatButtonWidgetAccessor::acquireLabel, flatButtonWidgetAccessor::changeLabel, "Raw label: ", Text.class));
				widgets.addSideMenuEntry("Label: " + label.getString());
			}
		}
	}

	@Override
	public boolean isDim2i(Object object) {
		return object instanceof Dim2i;
	}
	@Override
	public boolean hasDimensions(Object object) {
		return object instanceof FlatButtonWidget || object instanceof ControlElement;
	}
	@Override
	public boolean isCenteredFlatWidget(Object object) {
		return false;
	}

	@Override
	public int[] getDimensions(Object object) {
		if (object instanceof FlatButtonWidgetAccessor flatButtonWidgetAccessor) {
			return getDimensionsFromDim2i(flatButtonWidgetAccessor.getDim2i());
		} else if (object instanceof ControlElement<?> controlElement) {
			return getDimensionsFromDim2i(controlElement.getDimensions());
		} else if (object instanceof Dim2i dim2i) {
			return getDimensionsFromDim2i(dim2i);
		} else {
			throw new IllegalArgumentException("Object must be FlatButtonWidget, ControlElement, or Dim2i");
		}
	}

	private static int[] getDimensionsFromDim2i(Dim2i dim2i) {
		return new int[]{dim2i.x(), dim2i.y(), dim2i.width(), dim2i.height()};
	}
}
