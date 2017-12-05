package spirite.pc.ui.panel_layers.image_properties;

import spirite.base.brains.MasterControl;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.ImageWorkspace.MFlashObserver;
import spirite.base.image_data.ImageWorkspace.MNodeSelectionObserver;
import spirite.base.image_data.mediums.IMedium;
import spirite.base.image_data.mediums.PrismaticMedium;
import spirite.base.image_data.mediums.maglev.MaglevMedium;
import spirite.gui.hybrid.SPanel;

import java.awt.*;

public class MediumPropertiesPanel extends SPanel implements MNodeSelectionObserver, MFlashObserver{
	private final MasterControl master;
	private IMedium medium;
	private boolean yes = false;
	
	private final PrismaticImagePropertiesPanel pimg_panel;
	private final MaglevPropertiesPanel maglev_panel;
	
	public MediumPropertiesPanel(MasterControl master) {
		this.master = master;
		pimg_panel = new PrismaticImagePropertiesPanel(master);
		maglev_panel = new MaglevPropertiesPanel(master);
		
		master.addTrackingObserver(MNodeSelectionObserver.class, this);
		master.addTrackingObserver(MFlashObserver.class, this);

		this.setLayout(new GridLayout());
	}

	@Override
	public void selectionChanged(Node newSelection) {
		yes = false;
		ImageWorkspace ws = master.getCurrentWorkspace();
		if( ws != null) {
			BuildingMediumData bid = ws.buildActiveData();
			if( bid != null && bid.handle != null) {
				this.medium = ws.getData(bid.handle);
			}
			else this.medium = null;
			
			removeAll();
			if( medium instanceof PrismaticMedium) {
				pimg_panel.setMedium((PrismaticMedium)medium);
				add(pimg_panel);
			}
			else if( medium instanceof MaglevMedium) {
				maglev_panel.setMedium((MaglevMedium)medium);
				add(maglev_panel);
				
			}
		}
		
		repaint();
	}

	@Override
	public void flash() {
		repaint();
	}
}
