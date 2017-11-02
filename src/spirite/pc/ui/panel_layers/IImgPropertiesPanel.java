package spirite.pc.ui.panel_layers;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPanel;

import spirite.base.brains.MasterControl;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.ImageWorkspace.MFlashObserver;
import spirite.base.image_data.ImageWorkspace.MNodeSelectionObserver;
import spirite.base.image_data.images.IMedium;
import spirite.base.image_data.images.PrismaticMedium;
import spirite.base.image_data.images.PrismaticMedium.LImg;
import spirite.base.util.glmath.Rect;

public class IImgPropertiesPanel extends JPanel implements MNodeSelectionObserver, MFlashObserver{
	private final MasterControl master;
	private IMedium iimg;
	private boolean yes = false;
	
	public IImgPropertiesPanel(MasterControl master) {
		this.master = master;
		master.addTrackingObserver(MNodeSelectionObserver.class, this);
		master.addTrackingObserver(MFlashObserver.class, this);

		this.addMouseListener(mouser);
		this.addMouseMotionListener(mouser);
	}
	MouseAdapter mouser = new MouseAdapter() {
		public void mousePressed(MouseEvent e) {
			if( iimg instanceof PrismaticMedium) {
				PrismaticMedium piimg = (PrismaticMedium)iimg;
				List<LImg> colorLayers = piimg.getColorLayers();
				
				int index = getIndexFromPoint(e.getPoint());
				
				if( index >= 0 && index < colorLayers.size())
					draggingFromIndex = index;
				else 
					draggingFromIndex = -1;
			}
			
			repaint();
		}
		
		public void mouseDragged(MouseEvent e) {
			if( draggingFromIndex != -1 && iimg instanceof PrismaticMedium) {
				PrismaticMedium piimg = (PrismaticMedium)iimg;
				List<LImg> colorLayers = piimg.getColorLayers();
				
				int index = getIndexFromPoint(e.getPoint());
				
				if( index >= 0 && index < colorLayers.size())
					draggingToIndex = index;
				else 
					draggingToIndex = -1;
			}
			
			repaint();
		}
		
		public void mouseReleased(MouseEvent e) {
			if( draggingFromIndex != -1 && draggingToIndex != -1 
					&& draggingFromIndex != draggingToIndex && iimg instanceof PrismaticMedium) 
			{

				PrismaticMedium piimg = (PrismaticMedium)iimg;
				piimg.moveLayer( draggingFromIndex,draggingToIndex);
				master.getCurrentWorkspace().triggerFlash();
			}
			draggingToIndex = -1;
			draggingFromIndex = -1;
			repaint();
		}
	};

	int draggingFromIndex = -1;
	int draggingToIndex = -1;
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		if( iimg instanceof PrismaticMedium) {
			PrismaticMedium piimg = (PrismaticMedium)iimg;
			List<LImg> colorLayers = piimg.getColorLayers();
			
			int dx = 0;
			int dy = 0;
			for( int i=0; i<colorLayers.size(); ++i) {
				g.setColor(new Color(colorLayers.get(i).color));
				g.fillRect( dx + 1, dy + 1, 14, 14);

				if( draggingFromIndex == i) {
					g.setColor(Color.GRAY);
					g.drawRect(dx, dy, 15, 15);
				}if( draggingToIndex == i) {
					g.setColor(Color.BLACK);
					g.drawRect(dx, dy, 15, 15);
				}
				
				dx += 16;
				if( dx >= getWidth()) {
					dx = 0;
					dy += 16;
				}
			}
		}
	}
	
	private int getIndexFromPoint( Point p) {
		int w = Math.max(1, getWidth()/16);
		if( p.x < 0 || p.y < 0 || p.x > w*16 )
			return -1;
		
		return (p.x / 16) + (p.y / 16) * w;
	}
	private Rect getBoundsFromIndex( int i) {
		int w = Math.max(1, getWidth()/16);
		
		int dx = (i % w)*16;
		int dy = (i / w)*16;
		
		return new Rect(dx, dy, 16,16);
	}

	@Override
	public void selectionChanged(Node newSelection) {
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
