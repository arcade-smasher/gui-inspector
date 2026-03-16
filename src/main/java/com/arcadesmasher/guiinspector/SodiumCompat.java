package com.arcadesmasher.guiinspector;

import net.caffeinemc.mods.sodium.client.gui.Dimensioned;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.gui.widgets.FlatButtonWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;

public class SodiumCompat { // TODO: fully get rid of this and use accessor mixins

	public static boolean isDim2i(Object object) {
		return object instanceof Dim2i;
	}

	public static boolean isDimensioned(Object object) {
		return object instanceof Dimensioned;
	}

	public static boolean isAbstractWidget(Object object) {
		return object instanceof AbstractWidget;
	}

	/**
	 *
	 * @param object an Object that should be either a {@link Dimensioned} or {@link Dim2i}
	 * @return an {@code int[]} containing {x, y, width, height}
	 */
	public static int[] getDimensions(Object object) {
		if (object instanceof Dimensioned dimensioned) {
			Dim2i dim2i = dimensioned.getDimensions();
			return new int[]{dim2i.x(), dim2i.y(), dim2i.width(), dim2i.height()};
		} else if (object instanceof Dim2i(int x, int y, int width, int height)) {
			return new int[]{x, y, width, height};
		} else {
			throw new IllegalArgumentException("Object must be Dimensioned or Dim2i");
		}
	}

	/**
	 *
	 * @param object an Object that should be a {@link Dimensioned}
	 * @return an {@code int[]} containing {x, y, width, height}
	 */
	public static int[] getDimensionsFromDimensioned(Object object) {
		Dim2i dim2i = ((Dimensioned) object).getDimensions();
		return new int[]{dim2i.x(), dim2i.y(), dim2i.width(), dim2i.height()};
	}

	/**
	 *
	 * @param object an Object that should be a {@link Dim2i}
	 * @return an {@code int[]} containing {x, y, width, height}
	 */
	public static int[] getDimensionsFromDim2i(Object object) {
		Dim2i dim2i = (Dim2i) object;
		return new int[]{dim2i.x(), dim2i.y(), dim2i.width(), dim2i.height()};
	}

	/**
	 *
	 * @param object an Object that should be an {@link AbstractWidget}
	 * @return an {@code boolean} representing the output of {@code isHovered()} on the {@link AbstractWidget}
	 */
	public static boolean getHovered(Object object) {
		AbstractWidget abstractWidget = (AbstractWidget) object;
		return abstractWidget.isHovered();
	}

	/**
	 *
	 * @param object an Object that should be a {@link FlatButtonWidget}
	 * @return an {@code boolean} representing the output of {@code isHovered()} on the {@link FlatButtonWidget}
	 */
	public static boolean getVisible(Object object) {
		FlatButtonWidget flatButtonWidget = (FlatButtonWidget) object;
		return flatButtonWidget.isVisible();
	}
}
