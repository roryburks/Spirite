package spirite.gui.hybrid;

import javax.swing.JRadioButton;

import spirite.hybrid.Globals;

public class SRadioButton extends JRadioButton {
	public SRadioButton( String label) {
		super(label);
		this.setBackground(Globals.getColor("bg"));
		this.setForeground(Globals.getColor("text"));
	}

}
