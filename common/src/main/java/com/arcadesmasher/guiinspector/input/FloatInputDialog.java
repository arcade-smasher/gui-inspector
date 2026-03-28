package com.arcadesmasher.guiinspector.input;

import javax.swing.*;
import java.awt.*;
import java.text.ParseException;
import java.util.Optional;

public class FloatInputDialog extends InputDialog<Float> {

	private FloatInputDialog(Frame parent, String title, String label, float init, float min, float max) {
		super(parent, title);

		SpinnerNumberModel model = new SpinnerNumberModel(init, min, max, 0.1F);
		JSpinner spinner = new JSpinner(model);
		validateSpinner(spinner);

		add(new JLabel(label));
		add(spinner);

		submitButton.addActionListener(e -> {
			try {
				spinner.commitEdit();
			} catch (ParseException ex) {
				Toolkit.getDefaultToolkit().beep();
				return;
			}

			result = ((Number) spinner.getValue()).floatValue();
			dispose();
		});
	}

	public static Optional<Float> show(Frame parent, String title, String label, float init, float min, float max) {
		FloatInputDialog dialog = new FloatInputDialog(parent, title, label, init, min, max);
		return Optional.ofNullable(dialog.showDialog());
	}

	public static Optional<Float> show(Frame parent, String title, String label, float init) {
		return show(parent, title, label, init, Float.MIN_VALUE, Float.MAX_VALUE);
	}

	public static Optional<Float> show(Frame parent, String title, String label) {
		return show(parent, title, label, 0);
	}
}