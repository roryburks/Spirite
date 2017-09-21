package spirite.pc.ui.panel_layers;

import java.awt.GridLayout;

import javax.swing.JPanel;

import spirite.pc.ui.generic.BetterTree;

public class LayerAnimView extends JPanel {
	public LayerAnimView() {
		InitComponents();
	}
	
	private void InitComponents() {
    	this.setLayout(new GridLayout());
		this.add(new BetterTree());
	}
}
