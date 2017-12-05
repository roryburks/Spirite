package spirite.gui.hybrid;

import spirite.hybrid.Globals;

import javax.swing.*;

public class SCheckBox extends JCheckBox {
	public SCheckBox(String label) {
		this.setText(label);
		this.setBackground(Globals.getColor("bg"));
		this.setForeground(Globals.getColor("text"));
	}
}
