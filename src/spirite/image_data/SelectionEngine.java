package spirite.image_data;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import spirite.MUtil;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;

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
	
	ImageWorkspace workspace;
	// Variables related to Selection state
	private Selection selection = null;
	private boolean lifted = false;
	protected BufferedImage liftedData = null;
	private int startX;	// pulls double-duty as the starting point for the selection building
	private int startY;	// and the starting point for a MoveSelectionAction
	
	protected int offsetX;
	protected int offsetY;
	
	// Stored for the UndoEngine
	private int oldOX, oldOY;
	private Selection oldSelection;
	

	private boolean building = false;
	private SelectionType selectionType;	// Only used when building a selection
	
	SelectionEngine( ImageWorkspace workspace) {
		this.workspace = workspace;
	}

	public boolean isLifted() {
		return lifted;
	}
	
	
	public Selection getSelection() {
		return selection;
	}
	public int getOffsetX() {
		return offsetX;
	}
	public int getOffsetY() {
		return offsetY;
	}
	public void setOffset( int x, int y) {
		offsetX = x;
		offsetY = y;

		// Construct ImageChangeEvent and send it
		ImageChangeEvent evt = new ImageChangeEvent();
		evt.workspace = workspace;
		evt.selectionLayerChange = true;
		workspace.triggerImageRefresh(evt);
	}


	
	// :::: Selection Building
	public void startBuildingSelection( SelectionType type, int x, int y) {
		rememberOldSelection();
		
		
		// Anchor any in-the-air selection data
		if( lifted) {
			anchorLifted();
		}
		voidSelection();
		
		// Start building
		building = true;
		selectionType = type;
		startX = x;
		startY = y;
		selection = new NullSelection();
	}
	
	public void updateBuildingSelection( int x, int y) {
		if(!building) return;
		
		SelectionEvent evt = new SelectionEvent();
		
		switch( selectionType) {
		case RECTANGLE:
			offsetX = Math.min(startX, x);
			offsetY = Math.min(startY, y);
			selection = new RectSelection( Math.abs(startX-x), Math.abs(startY-y));
			evt.selection = selection;
			break;
		}
		
		triggerBuildingSelection( evt);
		
	}

	public void finishBuildingSelection() {
		building = false;
		if(selection == null) {
			voidSelection();
			return;
		}
		
		Rectangle rect = new Rectangle( 0, 0, workspace.getWidth(), workspace.getHeight());
		
		if( !selection.clipToRect(rect))
			voidSelection();
		
		UndoEngine undo = workspace.getUndoEngine();
		undo.storeAction( 
				undo.new SetSelectionAction(
						selection, offsetX, offsetY, 
						oldSelection, oldOX, oldOY), null);
		
		triggerBuildingSelection(null);
		triggerSelectionChanged(null);
	}
	private void rememberOldSelection() {
		if( selection == null)
			oldSelection = null;
		else
			oldSelection = (Selection)(selection.clone());
		oldOX = offsetX;
		oldOY = offsetY;
	}
	
	/** !! Should only be used by the UndoEngine !! */
	void setSelection( Selection selection, int offsetX, int offsetY) {
		voidSelection();
		
		this.selection = selection;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		
		triggerBuildingSelection(null);
		triggerSelectionChanged(null);
	}
	
	
	
	// ::::
	
	private void anchorLifted() {
		if( !lifted)
			return;

		BufferedImage layerImg = workspace.checkoutImage(selection.dataContext);
		
		Graphics g = layerImg.getGraphics();
		g.drawImage(liftedData, offsetX, offsetY, null);
		
		UndoEngine undoEngine = workspace.getUndoEngine();
//		undoEngine.storeAction(undoEngine.new MoveSelectionAction( startX, startY), selection.dataContext);
		workspace.checkinImage(selection.dataContext);
		voidSelection();
	}
	
	public void unselect() {
		UndoEngine undo = workspace.getUndoEngine();
		undo.storeAction( undo.new SetSelectionAction(null, 0, 0, selection, offsetX, offsetY), null);
		
		if( lifted)
			anchorLifted();
		
		
		selection = null;
	}
	
	/***
	 * Deletes the lifted data from memory.  Note: If you don't call this function
	 * any lifted data will automatically be merged with its dataContext when you
	 * start building a new selection.
	 */
	private void voidSelection() {
		building = false;
		selection = null;
		lifted = false;
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
		if( !selection.clipToRect(new Rectangle( 0, 0, workspace.getWidth(), workspace.getHeight()))) {
			voidSelection();
			return;
		}
		
		Rectangle rect = selection.getBounds();
		
		// At this point we're going through with the lift
		selection.dataContext = node.getImageData();
		
		// Creates a Selection Mask
		liftedData = new BufferedImage( 
				rect.width, 
				rect.height, 
				BufferedImage.TYPE_INT_ARGB);
		MUtil.clearImage(liftedData);
		
		BufferedImage layerImg = workspace.checkoutImage(selection.dataContext);
		
		// Copy the data inside the Selection's alphaMask to liftedData
		Graphics g = liftedData.getGraphics();
		selection.drawSelectionMask(g);
		Graphics2D g2 = (Graphics2D)g;
		g2.setComposite( AlphaComposite.getInstance(AlphaComposite.SRC_IN));
		g2.drawImage( layerImg, -offsetX, -offsetY, null);
		g.dispose();
		
		// Delete the data inside the selection's alphaMask from the lifted data
		g =layerImg.getGraphics();
		g2 = (Graphics2D)g;
		g2.translate(offsetX, offsetY);
		g2.setComposite( AlphaComposite.getInstance( AlphaComposite.DST_OUT));
		selection.drawSelectionMask(g2);
		g.dispose();
		
		lifted = true;
		
		startX = offsetX;
		startY = offsetY;
		
		workspace.checkinImage(node.getImageData());
	}
	
	// :::: Various Selection Formats
	public abstract class Selection 
	{
		protected ImageData dataContext;
		
		// Note: SelectionBounds is drawn in image-space (i.e. accounting
		//	for offsets), whereas SelectionMask is drawn in selection space
		//	(not accounting for offsets).
		public abstract void drawSelectionBounds( Graphics g);
		abstract void drawSelectionMask( Graphics g);
		public abstract boolean contains( int x, int y);
		abstract Rectangle getBounds();
		abstract boolean clipToRect( Rectangle rect);	// returns false if the clipped Selection is empty
		
		public ImageData getLiftedContext() {
			return dataContext;
		}
		
		public BufferedImage getLiftedData() {
			return liftedData;
		}
		
		public abstract Selection clone();
	}
	
	public class NullSelection extends Selection {
		@Override		public void drawSelectionBounds(Graphics g) {}
		@Override		void drawSelectionMask(Graphics g) {}
		@Override		public boolean contains(int x, int y) {return false;}
		@Override		Rectangle getBounds() {return new Rectangle(0,0,0,0);}
		@Override		boolean clipToRect(Rectangle rect) {return false;}
		@Override		public Selection clone() { return new NullSelection();}
	}
	
	/***
	 * Simple Rectangular Selection
	 */
	public class RectSelection extends Selection {
		int width;
		int height;
		RectSelection( int width, int height) {
			this.width = width;
			this.height = height;
		}
		@Override
		public void drawSelectionBounds( Graphics g) {
			g.drawRect( offsetX, offsetY, width, height);
		}
		@Override
		void drawSelectionMask( Graphics g) {
			g.setColor( Color.black);
			g.fillRect(0, 0, width, height);
		}
		@Override
		Rectangle getBounds() {
			return new Rectangle( 0, 0, width, height);
		}
		@Override
		public boolean contains( int x, int y) {
			return (new Rectangle( offsetX, offsetY, width, height)).contains(x,y);
		}
		
		@Override
		boolean clipToRect(Rectangle rect) {
			Rectangle selectionRect = new Rectangle( offsetX, offsetY, width, height);
			Rectangle intersection = rect.intersection(selectionRect);
			
			if( intersection.isEmpty())
				return false;
			
			offsetX = intersection.x;
			offsetY = intersection.y;
			width = intersection.width;
			height = intersection.height;
			
			return true;
		}
		

		@Override
		public RectSelection clone()  {
			return new RectSelection( width, height);
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
    public void addSelectionObserver( MSelectionEngineObserver obs) { selectionObservers.add(obs);}
	public void removeSelectionObserver( MSelectionEngineObserver obs) { selectionObservers.remove(obs); }
	
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

    
}
