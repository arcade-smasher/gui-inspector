package com.arcadesmasher.guiinspector.input;

import javax.swing.*;
import java.awt.*;
import java.text.ParseException;
import java.util.Optional;

public class DoubleInputDialog extends InputDialog<Double> {

	private DoubleInputDialog(Frame parent, String title, String label, double init, double min, double max) {
		super(parent, title);

		SpinnerNumberModel model = new SpinnerNumberModel(init, min, max, 0.1D);
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

			result = ((Number) spinner.getValue()).doubleValue();
			dispose();
		});
	}

	public static Optional<Double> show(Frame parent, String title, String label, double init, double min, double max) {
		DoubleInputDialog dialog = new DoubleInputDialog(parent, title, label, init, min, max);
		return Optional.ofNullable(dialog.showDialog());
	}

	public static Optional<Double> show(Frame parent, String title, String label, double init) {
		return show(parent, title, label, init, Double.MIN_VALUE, Double.MAX_VALUE);
	}

	public static Optional<Double> show(Frame parent, String title, String label) {
		return show(parent, title, label, 0);
	}
}