package spirite.gui.hybrid;

import spirite.hybrid.Globals;

import javax.swing.*;

public class SRadioButton extends JRadioButton {
	public SRadioButton( String label) {
		super(label);
		this.setBackground(Globals.getColor("bg"));
		this.setForeground(Globals.getColor("text"));
	}

}
