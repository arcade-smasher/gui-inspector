package com.arcadesmasher.guiinspector.input;

import javax.swing.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import java.awt.*;
import java.util.Optional;

public class TextInputDialog extends com.arcadesmasher.guiinspector.input.InputDialog<Component> {

	private TextInputDialog(Frame parent, String title, Component init) {
		super(parent, title);

		JTextField literalField = new JTextField(init.getString(), 20);
		JTextField translatableField = new JTextField((init.getContents() instanceof TranslatableContents translatable) ? translatable.getKey() : "", 20);
		translatableField.setEnabled(false);

		JCheckBox useTranslation = new JCheckBox("Use translation key instead");
		useTranslation.addActionListener(e -> {
			boolean translatable = useTranslation.isSelected();
			literalField.setEnabled(!translatable);
			translatableField.setEnabled(translatable);
		});

		add(new JLabel("Literal text:"));
		add(literalField);
		add(new JLabel("Translation key:"));
		add(translatableField);
		add(useTranslation);

		submitButton.addActionListener(e -> {
			if (useTranslation.isSelected()) {
				result = Component.translatable(translatableField.getText());
			} else {
				result = Component.literal(literalField.getText());
			}
			dispose();
		});
	}

	public static Optional<Component> show(Frame parent, String title, Component init) {
		TextInputDialog dialog = new TextInputDialog(parent, title, init);
		return Optional.ofNullable(dialog.showDialog());
	}

	public static Optional<Component> show(Frame parent, String title) {
		return show(parent, title, Component.empty());
	}
}