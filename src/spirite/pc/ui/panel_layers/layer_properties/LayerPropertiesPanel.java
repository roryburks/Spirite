package spirite.pc.ui.panel_layers.layer_properties;

import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import spirite.base.brains.MasterControl;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace.MNodeSelectionObserver;
import spirite.base.image_data.layers.Layer;
import spirite.base.image_data.layers.PuppetLayer;
import spirite.base.image_data.layers.ReferenceLayer;
import spirite.base.image_data.layers.SpriteLayer;
import spirite.pc.ui.omni.OmniFrame.OmniComponent;

public class LayerPropertiesPanel extends OmniComponent
	implements MNodeSelectionObserver
{
	final MasterControl master;
	final SpriteLayerPanel slp;
	final PuppetLayerPanel plp;
	final ReferenceLayerPanel rlp;
	JPanel nillJPanel = new JPanel();
	
	public LayerPropertiesPanel( MasterControl master) {
		this.master = master;
		slp = new SpriteLayerPanel(master);
		plp = new PuppetLayerPanel(master);
		rlp = new ReferenceLayerPanel(master);

		
		this.setLayout( new GridLayout());
		initComponent(nillJPanel);

		master.addTrackingObserver(MNodeSelectionObserver.class, this);
		this.selectionChanged(master.getCurrentWorkspace() == null  ? null : master.getCurrentWorkspace().getSelectedNode());
	}
	private void initComponent( JComponent comp) {
		this.removeAll();
		this.add(comp);
		this.repaint();
	}

	@Override
	public void selectionChanged(Node newSelection) {
		// NOTE: SwingUtilities.invokeLater is "needed" because this is being called
		//	from a selection change and will potentially cause new selection changes to 
		//	be created.  The alternative is to load all Panel types before they're needed

		if( newSelection instanceof LayerNode) {
			Layer layer = ((LayerNode) newSelection).getLayer();

			if( layer instanceof SpriteLayer) {
				slp.setRig((SpriteLayer)layer, master.getCurrentWorkspace());
				initComponent(slp);
				return;
			}
			if( layer instanceof PuppetLayer) {
				initComponent(plp);
				return;
			}
			if( layer instanceof ReferenceLayer) {
				rlp.setReference((ReferenceLayer)layer, master.getCurrentWorkspace());
				initComponent(rlp);
				return;
			}
		}

		initComponent(nillJPanel);
	}

}
