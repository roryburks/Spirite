package spirite.pc.ui.panel_layers.layer_properties;

import spirite.base.brains.MasterControl;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace.MNodeSelectionObserver;
import spirite.base.image_data.layers.Layer;
import spirite.base.image_data.layers.ReferenceLayer;
import spirite.base.image_data.layers.SpriteLayer;
import spirite.base.image_data.layers.puppet.PuppetLayer;
import spirite.gui.hybrid.SPanel;
import spirite.pc.ui.omni.OmniFrame.OmniComponent;

import javax.swing.*;
import java.awt.*;

public class LayerPropertiesPanel extends SPanel
	implements OmniComponent, MNodeSelectionObserver
{
	final MasterControl master;
	final SpriteLayerPanel slp;
	final PuppetLayerPanel plp;
	final ReferenceLayerPanel rlp;
	JComponent active = null;
	SPanel nillSPanel = new SPanel();
	
	public LayerPropertiesPanel( MasterControl master) {
		this.master = master;
		slp = new SpriteLayerPanel(master);
		plp = new PuppetLayerPanel(master);
		rlp = new ReferenceLayerPanel(master);

		
		this.setLayout( new GridLayout());
		initComponent(nillSPanel);

		master.addTrackingObserver(MNodeSelectionObserver.class, this);
		this.selectionChanged(master.getCurrentWorkspace() == null  ? null : master.getCurrentWorkspace().getSelectedNode());
	}
	private void initComponent( JComponent comp) {
		this.removeAll();
		this.add(comp);
		this.repaint();
		active = comp;
	}

	@Override
	public void selectionChanged(Node newSelection) {
		if( newSelection instanceof LayerNode) {
			Layer layer = ((LayerNode) newSelection).getLayer();

			if( layer instanceof SpriteLayer) {
				slp.setRig((SpriteLayer)layer, master.getCurrentWorkspace());
				initComponent(slp);
				return;
			}
			if( layer instanceof PuppetLayer) {
				plp.setPuppet((PuppetLayer)layer, master.getCurrentWorkspace());
				initComponent(plp);
				return;
			}
			if( layer instanceof ReferenceLayer) {
				rlp.setReference((LayerNode) newSelection, master.getCurrentWorkspace());
				initComponent(rlp);
				return;
			}
		}

		initComponent(nillSPanel);
	}

	@Override
	public void requestFocus() {
		if( active != null)
			active.requestFocus();
	}
	@Override public JComponent getComponent() {
		return this;
	}
}
