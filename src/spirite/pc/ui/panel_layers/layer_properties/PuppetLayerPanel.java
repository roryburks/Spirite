package spirite.pc.ui.panel_layers.layer_properties;

import javax.swing.JPanel;

import spirite.base.brains.MasterControl;
import spirite.base.image_data.layers.Puppet;

public class PuppetLayerPanel extends JPanel {
	private final MasterControl master;
	Puppet puppet;
	PuppetLayerPanel(MasterControl master) {
		this.master = master;
	}
}
