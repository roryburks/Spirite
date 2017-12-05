package spirite.gui.hybrid;

import spirite.hybrid.Globals;

import javax.swing.*;

public class SButton extends JButton {
	public SButton() {
		this(null);
	}
	public SButton(String str) {
		if( str != null)
			this.setText(str);
		this.setBackground(Globals.getColor("bgDark"));
		this.setForeground(Globals.getColor("text"));
		this.setBorder(BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, Globals.getColor("bevelBorderMed"), Globals.getColor("bevelBorderDark")));
	}
}
