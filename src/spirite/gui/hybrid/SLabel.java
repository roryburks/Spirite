package spirite.gui.hybrid;

import spirite.hybrid.Globals;

import javax.swing.*;

public class SLabel extends JLabel{
	public SLabel() {
		this(false);
	}
	public SLabel(boolean dark) {
		this(null, dark);
	}
	public SLabel(String string) {
		this(string,false);
	}

	public SLabel(String string,boolean dark) {
		if(string != null)
			this.setText(string);
		this.setForeground( Globals.getColor(dark ? "textDark" : "text"));
	}
}
