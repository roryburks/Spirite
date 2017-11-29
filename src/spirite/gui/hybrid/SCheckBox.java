package spirite.gui.hybrid;

import javax.swing.JCheckBox;

import spirite.hybrid.Globals;

public class SCheckBox extends JCheckBox {
	public SCheckBox(String label) {
		this.setText(label);
		this.setBackground(Globals.getColor("bg"));
		this.setForeground(Globals.getColor("text"));
	}
}
