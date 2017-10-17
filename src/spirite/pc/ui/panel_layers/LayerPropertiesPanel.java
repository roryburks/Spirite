package spirite.pc.ui.panel_layers;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import javax.swing.JPanel;

import spirite.base.brains.MasterControl;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.ImageWorkspace.MFlashObserver;
import spirite.base.image_data.ImageWorkspace.MSelectionObserver;
import spirite.base.image_data.images.IInternalImage;
import spirite.base.image_data.images.PrismaticInternalImage;
import spirite.base.image_data.images.PrismaticInternalImage.LImg;

public class LayerPropertiesPanel extends JPanel implements MSelectionObserver, MFlashObserver{
	private final MasterControl master;
	private IInternalImage iimg;
	boolean yes = false;
	
	public LayerPropertiesPanel(MasterControl master) {
		this.master = master;
		master.addTrackingObserver(MSelectionObserver.class, this);
		master.addTrackingObserver(MFlashObserver.class, this);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		if( iimg instanceof PrismaticInternalImage) {
			PrismaticInternalImage piimg = (PrismaticInternalImage)iimg;
			
			List<LImg> colorLayers = piimg.getColorLayers();
			for( int i=0; i<colorLayers.size(); ++i) {
				g.setColor(new Color(colorLayers.get(i).color));
				g.fillRect( 20*i + 2, 2, 18, 18);
			}
		}
	}

	@Override
	public void selectionChanged(Node newSelection) {
		System.out.println("TEST");
		yes = false;
		ImageWorkspace ws = master.getCurrentWorkspace();
		if( ws != null) {
			BuildingImageData bid = ws.buildActiveData();
			if( bid != null && bid.handle != null) {
				this.iimg = ws.getData(bid.handle);
			}
		}
		
		repaint();
	}

	@Override
	public void flash() {
		repaint();
	}
}
