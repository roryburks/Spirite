package spirite.image_data;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import spirite.MUtil;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;

/***
 *  The SelectionEngine controls the selected image data, moving it from
 *  layer to layer, workspace to workspace, and program-to-clipboard
 *  
 *  A "Selection" is essentially an alpha mask and a corresponding 
 *  BufferedImage which is floating outside of the ImageWorkspace until
 *  it is either merged into existing data or elevated to its own layer.
 *  
 * @author Rory Burks
 *
 */
public class SelectionEngine {
	public enum SelectionType {
		RECTANGLE,
		
	}
	
	
	
	BufferedImage liftedData = null;
	ImageWorkspace workspace;
	SelectionEngine( ImageWorkspace workspace) {
		this.workspace = workspace;
	}
	
	private Boolean building = false;
	private SelectionType selectionType;
	private int startX;
	private int startY;
	private Selection selection = null;
	public void startBuildingSelection( SelectionType type, int x, int y) {
		building = true;
		selectionType = type;
		startX = x;
		startY = y;
	}
	
	public void updateBuildingSelection( int x, int y) {
		SelectionEvent evt = new SelectionEvent();
		selection = new RectSelection(
				new Rectangle( 
					Math.min(startX, x), 
					Math.min(startY, y),
					Math.abs(startX-x),
					Math.abs(startY-y)));
		evt.selection = selection;
		
		triggerBuildingSelection( evt);
		
	}
	
	public void finishBuildingSelection() {
		building = false;
		if(selection == null) return;
		
		Rectangle r = new Rectangle( 0, 0, workspace.getWidth(), workspace.getHeight());
		
/*		Area a = new Area(selection);
		if( !a.intersects(r)) {
			selection = null;
		}*/
	}
	
	public Selection getSelection() {
		return selection;
	}
	
	public void liftSelection() {
		if( selection == null)
			return;
		
		Node snode = workspace.getSelectedNode();
		if( !(snode instanceof LayerNode))
			return;
		LayerNode node = (LayerNode)snode;
		
		// Determine if the proposed selection is within the bounds of the 
		// Workspace, if not, cancel lift.
		// TODO: Make this more precise intersection geometry.
		Rectangle workspaceRect = new Rectangle( 0, 0, workspace.getWidth(), workspace.getHeight());
		Rectangle rect = selection.getBounds().intersection(workspaceRect);
		
		if( rect.isEmpty())
			return;

		
		if( liftedData != null)
			liftedData.flush();
		
		// Creates a Selection Mask
		liftedData = new BufferedImage( rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
		MUtil.clearImage(liftedData);
		
		
		Graphics g = liftedData.getGraphics();
		selection.drawSelectionMask(g);
		Graphics2D g2 = (Graphics2D)g;
		g2.setComposite( AlphaComposite.getInstance(AlphaComposite.SRC_IN));
		g2.drawImage( node.getImageData().getData(), -rect.x, -rect.y, null);
		

		// !!!! DEBUG
		try {
			ImageIO.write(liftedData, "png", new File("E:/Test.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void anchorSelection( ImageData image) {
		// TODO
	}
	
	// :::: Various Selection Formats
	public abstract static class Selection {
		public abstract void drawSelectionBounds( Graphics g);
		abstract void drawSelectionMask( Graphics g);
		abstract Rectangle getBounds();
	}
	
	public static class RectSelection extends Selection {
		Rectangle rect;
		RectSelection( Rectangle rect) {
			this.rect = rect;
		}
		@Override
		public void drawSelectionBounds( Graphics g) {
			g.drawRect( rect.x, rect.y, rect.width, rect.height);
		}
		@Override
		void drawSelectionMask( Graphics g) {
			g.setColor( Color.black);
			g.fillRect(0, 0, rect.width, rect.height);
		}
		@Override
		Rectangle getBounds() {
			return (Rectangle) rect.clone();
		}
	}

	// :::: Observers
	public static interface MSelectionEngineObserver {
		public void selectionBuilt(SelectionEvent evt);
		public void buildingSelection( SelectionEvent evt);
	}
	public static class SelectionEvent {
		Selection selection;
	}
    List<MSelectionEngineObserver> selectionObservers = new ArrayList<>();

    void triggerSelectionChanged(SelectionEvent evt) {
    	for( MSelectionEngineObserver obs : selectionObservers) {
    		obs.selectionBuilt(evt);
    	}
    }
    void triggerBuildingSelection(SelectionEvent evt) {
    	for( MSelectionEngineObserver obs : selectionObservers) {
    		obs.buildingSelection(evt);
    	}
    }

    public void addSelectionObserver( MSelectionEngineObserver obs) { selectionObservers.add(obs);}
	public void removeSelectionObserver( MSelectionEngineObserver obs) { selectionObservers.remove(obs); }
	    
}
