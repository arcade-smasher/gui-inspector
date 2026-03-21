package com.arcadesmasher.guiinspector.input;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import javax.swing.*;
import java.awt.*;
import java.text.ParseException;
import java.util.Optional;

public class ItemStackInputDialog extends InputDialog<ItemStack> {

	private ItemStackInputDialog(Frame parent, String title, ItemStack init) {
		super(parent, title);

		JComboBox<String> comboBox = new JComboBox<>(Registries.ITEM.getIds().stream().map(Identifier::toString).toArray(String[]::new));
		comboBox.setEditable(true);
		comboBox.setSelectedItem(Registries.ITEM.getId(init.getItem()).toString());

		add(new JLabel("Enter count: "));
		SpinnerNumberModel model = new SpinnerNumberModel(init.getCount(), Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
		JSpinner spinner = new JSpinner(model);
		add(spinner);
		validateSpinner(spinner);
		add(new JLabel("Enter item: "));
		add(comboBox);
		validateComboBox(comboBox);

		submitButton.addActionListener(e -> {
			try {
				spinner.commitEdit();
			} catch (ParseException ex) {
				Toolkit.getDefaultToolkit().beep();
				return;
			}
			Object selected = comboBox.getSelectedItem();
			if (selected instanceof String str) {
				try {
					Identifier id = Identifier.of(str);
					result = Registries.ITEM.get(id).getDefaultStack();
					result.setCount((Integer) spinner.getValue());
				} catch (Exception ex) {
					Toolkit.getDefaultToolkit().beep();
					return;
				}
			}
			dispose();
		});
	}

	public static Optional<ItemStack> show(Frame parent, String title, ItemStack init) {
		ItemStackInputDialog dialog = new ItemStackInputDialog(parent, title, init);
		return Optional.ofNullable(dialog.showDialog());
	}

	public static Optional<ItemStack> show(Frame parent, String title) {
		return show(parent, title, Items.AIR.getDefaultStack());
	}
}