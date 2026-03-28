package com.arcadesmasher.guiinspector;

import com.arcadesmasher.guiinspector.data.rowdata.DefaultRowData;
import com.arcadesmasher.guiinspector.mixin.newsodium.CenteredFlatWidgetAccessor;
import com.arcadesmasher.guiinspector.mixin.newsodium.FlatButtonWidgetAccessor;
import com.arcadesmasher.guiinspector.tree.TreePanel;
import net.caffeinemc.mods.sodium.client.gui.Dimensioned;
import net.caffeinemc.mods.sodium.client.gui.widgets.CenteredFlatWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.text.Text;

public class SodiumCompatNew implements SodiumCompat {

	@Override
	public void addSodiumEntries(TreePanel widgets, Object object) {
		if (object instanceof AbstractWidgetAccessor abstractWidgetAccessor) {
			widgets.addSideMenuEntry("");
			widgets.addSideMenuEntry(new DefaultRowData<>(abstractWidgetAccessor::getHovered, abstractWidgetAccessor::setHovered, "Hovered: ", Boolean.class));
		}
		if (object instanceof CenteredFlatWidgetAccessor centeredFlatWidgetAccessor) {
			widgets.addSideMenuEntry("");
			widgets.addSideMenuEntry(new DefaultRowData<>(centeredFlatWidgetAccessor::isVisible, centeredFlatWidgetAccessor::invokeSetVisible, "Visible: ", Boolean.class));
			widgets.addSideMenuEntry(new DefaultRowData<>(centeredFlatWidgetAccessor::isSelected, centeredFlatWidgetAccessor::invokeSetSelected, "Selected: ", Boolean.class));
			widgets.addSideMenuEntry(new DefaultRowData<>(centeredFlatWidgetAccessor::isSelectable, centeredFlatWidgetAccessor::setIsSelectable, "Selectable: ", Boolean.class));
			widgets.addSideMenuEntry(new DefaultRowData<>(centeredFlatWidgetAccessor::isEnabled, centeredFlatWidgetAccessor::invokeSetEnabled, "Enabled: ", Boolean.class));
			Text label = centeredFlatWidgetAccessor.getLabel();
			if (label != null) {
				widgets.addSideMenuEntry(new DefaultRowData<>(centeredFlatWidgetAccessor::getLabel, centeredFlatWidgetAccessor::setLabel, "Raw label: ", Text.class));
				widgets.addSideMenuEntry("Label: " + label.getString());
			}
			Text subtitle = centeredFlatWidgetAccessor.getSubtitle();
			if (subtitle != null) {
				widgets.addSideMenuEntry(new DefaultRowData<>(centeredFlatWidgetAccessor::getSubtitle, centeredFlatWidgetAccessor::setSubtitle, "Raw subtitle: ", Text.class));
				widgets.addSideMenuEntry("Subtitle: " + subtitle.getString());
			}
		}
		if (object instanceof FlatButtonWidgetAccessor flatButtonWidgetAccessor) {
			widgets.addSideMenuEntry("");
			widgets.addSideMenuEntry(new DefaultRowData<>(flatButtonWidgetAccessor::getVisible, flatButtonWidgetAccessor::invokeSetVisible, "Visible: ", Boolean.class));
			widgets.addSideMenuEntry(new DefaultRowData<>(flatButtonWidgetAccessor::isSelected, flatButtonWidgetAccessor::invokeSetSelected, "Selected: ", Boolean.class));
			widgets.addSideMenuEntry(new DefaultRowData<>(flatButtonWidgetAccessor::isEnabled, flatButtonWidgetAccessor::invokeSetEnabled, "Enabled: ", Boolean.class));
			widgets.addSideMenuEntry(new DefaultRowData<>(flatButtonWidgetAccessor::shouldDrawBackground, flatButtonWidgetAccessor::setShouldDrawBackground, "Draw background: ", Boolean.class));
			widgets.addSideMenuEntry(new DefaultRowData<>(flatButtonWidgetAccessor::shouldDrawFrame, flatButtonWidgetAccessor::setShouldDrawFrame, "Draw frame: ", Boolean.class));
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
		return object instanceof Dimensioned;
	}
	@Override
	public boolean isCenteredFlatWidget(Object object) {
		return object instanceof CenteredFlatWidget;
	}

	@Override
	public int[] getDimensions(Object object) {
		if (object instanceof Dimensioned dimensioned) {
			return getDimensionsFromDimensioned(dimensioned);
		} else if (object instanceof Dim2i dim2i) {
			return getDimensionsFromDimensioned(dim2i);
		} else {
			throw new IllegalArgumentException("Object must be Dimensioned or Dim2i");
		}
	}

	public int[] getDimensionsFromDimensioned(Object object) {
		return getDimensionsFromDimensioned((Dimensioned) object);
	}
	private static int[] getDimensionsFromDimensioned(Dimensioned dimensioned) {
		return getDimensionsFromDim2i(dimensioned.getDimensions());
	}

	public int[] getDimensionsFromDim2i(Object object) {
		return getDimensionsFromDim2i((Dim2i) object);
	}
	private static int[] getDimensionsFromDim2i(Dim2i dim2i) {
		return new int[]{dim2i.x(), dim2i.y(), dim2i.width(), dim2i.height()};
	}
}
