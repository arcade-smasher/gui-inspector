package com.arcadesmasher.guiinspector.input;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public class StringInputDialog extends InputDialog<String> {

	private StringInputDialog(Frame parent, String title, String label, String init) {
		super(parent, title);

		JTextField textField = new JTextField(init, 20);

		add(new JLabel(label));
		add(textField);

		submitButton.addActionListener(e -> {
			result = textField.getText();
			dispose();
		});
	}

	public static Optional<String> show(Frame parent, String title, String label, String init) {
		StringInputDialog dialog = new StringInputDialog(parent, title, label, init);
		return Optional.ofNullable(dialog.showDialog());
	}

	public static Optional<String> show(Frame parent, String title, String label) {
		return show(parent, title, label, "");
	}
}