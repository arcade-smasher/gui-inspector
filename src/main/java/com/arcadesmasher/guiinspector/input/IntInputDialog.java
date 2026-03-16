package com.arcadesmasher.guiinspector.input;

import javax.swing.*;
import java.awt.*;
import java.text.ParseException;
import java.util.Optional;

public class IntInputDialog extends InputDialog<Integer> {

	private IntInputDialog(Frame parent, String title, String label, int init, int min, int max) {
		super(parent, title);

		SpinnerNumberModel model = new SpinnerNumberModel(init, min, max, 1);
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

			result = (Integer) spinner.getValue();
			dispose();
		});
	}

	public static Optional<Integer> show(Frame parent, String title, String label, int init, int min, int max) {
		IntInputDialog dialog = new IntInputDialog(parent, title, label, init, min, max);
		return Optional.ofNullable(dialog.showDialog());
	}

	public static Optional<Integer> show(Frame parent, String title, String label, int init) {
		return show(parent, title, label, init, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	public static Optional<Integer> show(Frame parent, String title, String label) {
		return show(parent, title, label, 0);
	}
}