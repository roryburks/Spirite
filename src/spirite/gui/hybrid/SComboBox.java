package spirite.gui.hybrid;

import spirite.hybrid.Globals;

import javax.swing.*;

public class SComboBox<E> extends JComboBox<E> {
	public SComboBox() {
		super();
		init();
	}
	
	public SComboBox(E[] things) {
		super(things);
		init();
	}
	
	private void init() {
		this.setBackground(Globals.getColor("fg"));
		this.setForeground(Globals.getColor("textDark"));
	}
}
