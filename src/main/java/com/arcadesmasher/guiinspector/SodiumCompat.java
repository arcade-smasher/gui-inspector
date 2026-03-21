package com.arcadesmasher.guiinspector;

import net.caffeinemc.mods.sodium.client.gui.Dimensioned;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.gui.widgets.CenteredFlatWidget;
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
	public static boolean isCenteredFlatWidget(Object object) {
		return object instanceof CenteredFlatWidget;
	}
	public static boolean isFlatButtonWidget(Object object) {
		return object instanceof FlatButtonWidget;
	}

	/**
	 *
	 * @param object an Object that should be either a {@link Dimensioned} or {@link Dim2i}
	 * @return an {@code int[]} containing {x, y, width, height}
	 */
	public static int[] getDimensions(Object object) {
		if (object instanceof Dimensioned dimensioned) {
			return getDimensionsFromDimensioned(dimensioned);
		} else if (object instanceof Dim2i dim2i) {
			return getDimensionsFromDimensioned(dim2i);
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
		return getDimensionsFromDimensioned((Dimensioned) object);
	}
	private static int[] getDimensionsFromDimensioned(Dimensioned dimensioned) {
		return getDimensionsFromDim2i(dimensioned.getDimensions());
	}

	/**
	 *
	 * @param object an Object that should be a {@link Dim2i}
	 * @return an {@code int[]} containing {x, y, width, height}
	 */
	public static int[] getDimensionsFromDim2i(Object object) {
		return getDimensionsFromDim2i((Dim2i) object);
	}
	private static int[] getDimensionsFromDim2i(Dim2i dim2i) {
		return new int[]{dim2i.x(), dim2i.y(), dim2i.width(), dim2i.height()};
	}
}
