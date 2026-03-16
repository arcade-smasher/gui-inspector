package com.arcadesmasher.guiinspector.input;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public class BooleanInputDialog extends InputDialog<Boolean> {

	private BooleanInputDialog(Frame parent, String title, String label, boolean init) {
		super(parent, title);

		JCheckBox checkBox = new JCheckBox(label, init);

		add(checkBox);

		submitButton.addActionListener(e -> {
			result = checkBox.isSelected();
			dispose();
		});
	}

	public static Optional<Boolean> show(Frame parent, String title, String label, boolean init) {

		BooleanInputDialog dialog = new BooleanInputDialog(parent, title, label, init);

		return Optional.ofNullable(dialog.showDialog());
	}

	public static Optional<Boolean> show(Frame parent, String title, String label) {

		return show(parent, title, label, false);
	}
}