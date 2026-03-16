package com.arcadesmasher.guiinspector.input;

import javax.swing.*;
import java.awt.*;
import java.text.ParseException;

public abstract class InputDialog<T> extends JDialog {

	protected JButton submitButton = new JButton("OK");
	protected JButton cancelButton = new JButton("Cancel");

	protected T result = null;

	protected InputDialog(Frame parent, String title) {
		super(parent, title, true); // modal
		setLayout(new FlowLayout());

		cancelButton.addActionListener(e -> {
			result = null;
			dispose();
		});

		add(submitButton);
		add(cancelButton);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	protected void validateSpinner(JSpinner spinner) {
		((JSpinner.NumberEditor) spinner.getEditor()).getTextField().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { SwingUtilities.invokeLater(this::checkValid); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { SwingUtilities.invokeLater(this::checkValid); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { SwingUtilities.invokeLater(this::checkValid); }

			private void checkValid() {
				try {
					spinner.commitEdit();
					submitButton.setEnabled(true);
				} catch (ParseException ex) {
					submitButton.setEnabled(false);
				}
			}
		});
	}

	protected T showDialog() {
		pack();
		setLocationRelativeTo(getOwner());
		setVisible(true);
		return result;
	}
}