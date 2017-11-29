package spirite.gui.hybrid;

import javax.swing.JComboBox;

import spirite.hybrid.Globals;

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
