package com.arcadesmasher.guiinspector.input;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public class TextInputDialog extends InputDialog<Text> {

	private TextInputDialog(Frame parent, String title, Text init) {
		super(parent, title);

		JTextField literalField = new JTextField(init.getString(), 20);
		JTextField translatableField = new JTextField((init.getContent() instanceof TranslatableTextContent translatable) ? translatable.getKey() : "", 20);
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
				result = Text.translatable(translatableField.getText());
			} else {
				result = Text.literal(literalField.getText());
			}
			dispose();
		});
	}

	public static Optional<Text> show(Frame parent, String title, Text init) {
		TextInputDialog dialog = new TextInputDialog(parent, title, init);
		return Optional.ofNullable(dialog.showDialog());
	}

	public static Optional<Text> show(Frame parent, String title) {
		return show(parent, title, Text.empty());
	}
}