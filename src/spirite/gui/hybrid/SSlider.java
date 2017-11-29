package spirite.gui.hybrid;

import javax.swing.JSlider;

import spirite.hybrid.Globals;

public class SSlider extends JSlider {
	public SSlider() {
		this.setBackground(Globals.getColor("bg"));
		this.setForeground(Globals.getColor("fg"));
	}
}
