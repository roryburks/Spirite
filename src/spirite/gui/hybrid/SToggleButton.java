package spirite.gui.hybrid;

import javax.swing.BorderFactory;
import javax.swing.JToggleButton;

import spirite.hybrid.Globals;

public class SToggleButton extends JToggleButton {

	public SToggleButton() {
		init();
	}
	public SToggleButton(String str) {
		super(str);
		init();
	}
	
	private void init() {
		this.setBackground(Globals.getColor("bgDark"));
		this.setForeground(Globals.getColor("text"));
		this.setBorder(BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, Globals.getColor("bevelBorderMed"), Globals.getColor("bevelBorderDark")));
	}
}
