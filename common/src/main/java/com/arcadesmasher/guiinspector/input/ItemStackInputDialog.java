package com.arcadesmasher.guiinspector.input;

import imgui.ImGui;
import imgui.type.ImInt;
import imgui.type.ImString;
import net.minecraft.core.registries.BuiltInRegistries;

//#if _1_20_1
//$$ import net.minecraft.resources.ResourceLocation;
//#elif _1_21_11
import net.minecraft.resources.Identifier;
//#endif
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;
import java.util.function.Consumer;

public class ItemStackInputDialog extends InputDialog<ItemStack> {

	private final ImInt count;
	private final ImString itemId;

	protected ItemStackInputDialog(String title, String label, ItemStack initial, Consumer<Optional<ItemStack>> callback) {
		super(title, label, initial, callback);

		count = new ImInt(initial.getCount());

		String id = BuiltInRegistries.ITEM.getKey(initial.getItem()).toString();
		itemId = new ImString(id, 256);

		updateValue();
	}

	public static void show(String title, String label, ItemStack initial, Consumer<Optional<ItemStack>> callback) {
		new ItemStackInputDialog(title, label, initial, callback).open();
	}

	public static void show(String title, String label, Consumer<Optional<ItemStack>> callback) {
		show(title, label, Items.AIR.getDefaultInstance(), callback);
	}

	@Override
	protected void renderFields() {
		ImGui.text("Enter count:");
		ImGui.setNextItemWidth(160);

		if (ImGui.inputInt("##count", count)) {
			updateValue();
		}

		ImGui.text("Enter item:");
		ImGui.setNextItemWidth(300);

		if (ImGui.inputText("##item", itemId)) {
			updateValue();
		}
	}

	@Override
	protected boolean isValid() {
		return resolveItem() != null;
	}

	private void updateValue() {
		ItemStack stack = resolveItem();

		if (stack != null) {
			stack.setCount(count.get());
			value = stack;
		}
	}

	private ItemStack resolveItem() {
		try {
			//#if _1_20_1
//$$			ResourceLocation id = ResourceLocation.of(itemId.get(), ':');
//$$			return BuiltInRegistries.ITEM.get(id).getDefaultInstance();
			//#elif _1_21_11
			Identifier id = Identifier.bySeparator(itemId.get(), ':');
			return BuiltInRegistries.ITEM.getValue(id).getDefaultInstance();
			//#endif
		} catch (Exception ignored) {
			return null;
		}
	}
}