package spirite.pc.ui.panel_layers.image_properties;

import java.awt.Graphics;

import javax.swing.JLabel;
import javax.swing.JPanel;

import spirite.base.brains.MasterControl;
import spirite.base.image_data.mediums.maglev.MaglevMedium;

public class MaglevPropertiesPanel extends JPanel {
	private final MasterControl master;
	
	private final JLabel label = new JLabel();
	MaglevMedium medium;
	
	MaglevPropertiesPanel( MasterControl master){
		this.master = master;
		
		this.add(label);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		StringBuilder sb = new StringBuilder();
		
		if( medium != null) {
			sb.append("Number of things: ");
			sb.append(medium.getThings().size());
		}
		
		label.setText(sb.toString());
		super.paintComponent(g);
	}

	public void setMedium(MaglevMedium medium) {
		this.medium = medium;
	}
}
