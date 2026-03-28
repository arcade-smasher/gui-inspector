package com.arcadesmasher.guiinspector;

import net.minecraft.client.gui.widget.Widget;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WidthHeightRegistry {

	private static final Map<Key, BiConsumer<Widget, Integer>> CACHE = new ConcurrentHashMap<>();

	public static Consumer<Integer> getSetter(Enum widthHeight, Widget widget) {
		if (widget instanceof WidthHeightAccessor accessor) {
			return widthHeight == Enum.WIDTH
					? accessor::setWidth
					: accessor::setHeight;
		}

		BiConsumer<Widget, Integer> setter =
				CACHE.computeIfAbsent(
						new Key(widget.getClass(), widthHeight),
						WidthHeightRegistry::reflectSetter
				);

		if (setter == null) return null;

		return value -> setter.accept(widget, value);
	}

	private static BiConsumer<Widget, Integer> reflectSetter(Key key) {
		try {
			Field field = key.clazz.getDeclaredField(key.type.getValue());
			field.setAccessible(true);

			if (!Integer.class.isAssignableFrom(field.getType())
					&& field.getType() != int.class) {
				return null;
			}

			return (widget, value) -> {
				try {
					field.set(widget, value);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			};

		} catch (NoSuchFieldException e) {
			return null;
		}
	}

	private record Key(Class<?> clazz, Enum type) {}

	public enum Enum {
		WIDTH("width"),
		HEIGHT("height");

		private final String widthHeight;

		Enum(String width) {
			this.widthHeight = width;
		}

		public String getValue() {
			return this.widthHeight;
		}
	}
}
