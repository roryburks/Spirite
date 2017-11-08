package spirite.base.image_data.selection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.UndoEngine;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.UndoEngine.ImageAction;
import spirite.base.image_data.UndoEngine.NullAction;
import spirite.base.image_data.UndoEngine.StackableAction;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IAnchorLiftModule;
import spirite.base.image_data.mediums.drawer.IImageDrawer.ILiftSelectionModule;
import spirite.base.pen.selection_builders.RectSelectionBuilder;
import spirite.base.util.Colors;
import spirite.base.util.ObserverHandler;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2i;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.HybridUtil;
import spirite.hybrid.HybridUtil.UnsupportedImageTypeException;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;

/***
 *  The SelectionEngine controls the selected image data, moving it from
 *  layer to layer, workspace to workspace, and program-to-clipboard
 *  
 *  A "Selection" is essentially an alpha mask and a corresponding 
 *  BufferedImage which is floating outside of the ImageWorkspace until
 *  it is either merged into existing data or elevated to its own layer.
 *  
 *  I debated on whether or not to integrate offsets into the Selection
 *  rather than have it tracked separately, but offsets would still need
 *  to be tracked for liftedData so there would be no point.
 *  
 * @author Rory Burks
 *
 */
public class SelectionEngine {
	private final ImageWorkspace workspace;
	private final UndoEngine undoEngine;	
	
	
	// Variables relating to Building
	public enum BuildMode {
		DEFAULT, ADD, SUBTRACT, INTERSECTION
	};
	
	// Variables relating to Transforming
	private SelectionMask selection = null;
	private ALiftedData lifted = null;
	
	private boolean proposingTransform = false;
	MatTrans proposedTransform = null;

	
	public SelectionEngine( ImageWorkspace workspace) {
		this.workspace = workspace;
		this.undoEngine = workspace.getUndoEngine();

		workspace.triggerSelectionRefresh();
	}
	
	// ============
	// ==== Basic Gets
	public SelectionMask getSelection() {return selection;}
	public boolean isLifted() {return lifted != null;}
	public ALiftedData getLiftedData() {return lifted;}
	
	// =======
	// ==== Basic and complicated Selection settings
	public void setSelection( final SelectionMask newSelection) {
		final SelectionMask oldSelection = selection;
		finalizeSelection();
		undoEngine.performAndStore( new NullAction() {
			@Override
			protected void undoAction() {
				selection = oldSelection;
				workspace.triggerSelectionRefresh();
			}
			
			@Override
			protected void performAction() {
				selection = newSelection;
				workspace.triggerSelectionRefresh();
			}
			@Override public String getDescription() {
				return "Change Selection";
			}
		});
	}
	public void mergeSelection(SelectionMask newSelection, BuildMode mode) {
		
		switch( mode) {
		case DEFAULT:
			setSelection(newSelection);
			break;
		case ADD:
			setSelection(addSelection(selection, newSelection));
			break;
		case SUBTRACT:
			setSelection(subtractSelection(selection, newSelection));
			break;
		case INTERSECTION:
			setSelection(intersectSelection(selection, newSelection));
			break;
		}
	}
	public SelectionMask addSelection( SelectionMask sel1, SelectionMask sel2) {
		Rect area;
		Rect area1 = (sel1 == null) ? null : new Rect(sel1.ox, sel1.oy, sel1.getWidth(), sel1.getHeight());
		Rect area2 = (sel2 == null) ? null : new Rect(sel2.ox, sel2.oy, sel2.getWidth(), sel2.getHeight());
		
		if( area1 == null && area2 == null)
			return null;
		
		area = (area1 == null)? area2 : area1.union(area2);
		
		RawImage image = HybridHelper.createImage(area.width, area.height);

		GraphicsContext gc = image.getGraphics();
		gc.preTranslate( -area.x, -area.y);
		if( sel1 != null)
			sel1.drawMask(gc, true);
		if( sel2 != null)
			sel2.drawMask(gc, true);
		
		return new SelectionMask(image, area.x, area.y);
	}
	public SelectionMask subtractSelection( SelectionMask sel1, SelectionMask sel2) {
		if( sel1 == null) return null;
		
		Rect area = new Rect(sel1.ox, sel1.oy, sel1.getWidth(), sel1.getHeight());
		// No need to union with sel2
		
		RawImage image = HybridHelper.createImage(area.width, area.height);

		GraphicsContext gc = image.getGraphics();
		gc.preTranslate( -area.x, -area.y);
		if( sel1 != null)
			sel1.drawMask(gc, true);
		gc.setComposite( Composite.DST_OUT, 1.0f);
		if( sel2 != null)
			sel2.drawMask(gc, true);

		return new SelectionMask(image, area.x, area.y);
	}
	public SelectionMask intersectSelection( SelectionMask sel1, SelectionMask sel2) {
		if( sel1 == null || sel2 == null) return null;
		
		Rect area = (new Rect(sel1.ox, sel1.oy, sel1.getWidth(), sel1.getHeight()))
				.intersection(new Rect(sel2.ox, sel2.oy, sel2.getWidth(), sel2.getHeight()));
		
		if( area == null || area.isEmpty()) return null;

		RawImage img = HybridHelper.createImage(area.width, area.height);
		
		GraphicsContext gc = img.getGraphics();
		gc.preTranslate(-area.x, -area.y);
		sel2.drawMask(gc, true);
		gc.setComposite(Composite.DST_IN, 1.0f);
		sel1.drawMask(gc, true);
		//gc.drawImage(img1, 0, 0);
		
		return new SelectionMask(img, area.x, area.y);
	}
	public SelectionMask invertSelection( SelectionMask sel) {
		if( sel == null) return null;
		
		Rect area = new Rect(0,0,workspace.getWidth(), workspace.getHeight());
		
		RawImage image = HybridHelper.createImage( area.width, area.height);
		GraphicsContext gc = image.getGraphics();
		gc.setColor(Colors.WHITE);
		gc.fillRect(0, 0, area.width, area.height);
		gc.setComposite(Composite.DST_OUT,  1.0f);
		sel.drawMask(gc, true);
		
		return new SelectionMask(image);
	}
	public SelectionMask buildRectSelection( Rect rect) {
		RectSelectionBuilder rsb = new RectSelectionBuilder(workspace);
		rsb.start(rect.x, rect.y);
		rsb.update(rect.x+rect.width, rect.y+rect.height);
		return new SelectionMask( rsb.build());
	}
	private void modifySelection( int ox, int oy) {
		undoEngine.performAndStore(new ModifyingSelection(ox, oy));
	}
	class ModifyingSelection extends NullAction implements StackableAction 
	{
		int oldOx, oldOy, newOx, newOy;
		
		public ModifyingSelection( int newOx, int newOy) {
			this.newOx = newOx;
			this.newOy = newOy;
			this.oldOx = selection.ox;
			this.oldOy = selection.oy;
		}
		
		@Override protected void performAction() {
			selection = new SelectionMask(selection, newOx, newOy);
			workspace.triggerSelectionRefresh();
		}
		@Override protected void undoAction() {
			selection = new SelectionMask(selection, oldOx, oldOy);
			workspace.triggerSelectionRefresh();
		}
		@Override public boolean canStack(UndoableAction newAction) {
			return newAction instanceof ModifyingSelection;
		}
		@Override public void stackNewAction(UndoableAction newAction) {
			newOx = ((ModifyingSelection)newAction).newOx;
			newOy = ((ModifyingSelection)newAction).newOy;
		}
		@Override
		public String getDescription() {
			return "Moving Selection";
		}
	}
	
	// =======
	// === Transforming Lifted Data
	MatTrans liftedTrans = new MatTrans();
	public MatTrans getLiftedDrawTrans() {
		MatTrans trans = new MatTrans();
		trans.preTranslate(-lifted.getWidth()/2, -lifted.getHeight()/2);
		trans.preConcatenate(liftedTrans);
		int dx = (selection == null) ? 0 : selection.ox;
		int dy = (selection == null) ? 0 : selection.oy;
		trans.preTranslate(lifted.getWidth()/2 + dx, lifted.getHeight()/2 + dy);
		
		return trans;
	}
	
	public void setOffset(int ox, int oy) {
		if( !isLifted()) {
			attemptLiftData( workspace.getSelectedNode());
		}
		
		modifySelection(ox, oy);
	}
	
	// ============
	// ==== Lifting Data
	public void attemptLiftData( final Node node ) {
		IImageDrawer drawer =  workspace.getDrawerFromNode( node);
		
		if( drawer instanceof ILiftSelectionModule) {
			final ALiftedData newLifted = ((ILiftSelectionModule) drawer).liftSelection(selection);
			final ALiftedData oldLifted = lifted;
			
			List<UndoableAction> actions = new ArrayList<>(2);
			
			actions.add(new NullAction() {
				@Override
				protected void performAction() {
					lifted = newLifted;
				}

				@Override
				protected void undoAction() {
					lifted = oldLifted;
				}
			});
			actions.add( new ModifyingSelection(selection.ox, selection.oy));
			
			undoEngine.performAndStore(undoEngine.new StackableCompositeAction(actions, "Lift and Move Data"));
		}
		else
			HybridHelper.beep();
	}
	
	// =========
	// ==== Automatic
	private void finalizeSelection() {
		if( isLifted()) {
			IImageDrawer drawer = workspace.getActiveDrawer();
			
			if( drawer instanceof IAnchorLiftModule && ((IAnchorLiftModule) drawer).acceptsLifted(lifted)) {
				((IAnchorLiftModule)drawer).anchorLifted(lifted, getLiftedDrawTrans());
			}
			else 
				HybridHelper.beep();
			
			lifted = null;
		}
	}
	

	// ============
	// ==== Observers
	private final ObserverHandler<MSelectionEngineObserver> selectionObs = new ObserverHandler<>();
    public void addSelectionObserver( MSelectionEngineObserver obs) { selectionObs.addObserver(obs);}
	public void removeSelectionObserver( MSelectionEngineObserver obs) { selectionObs.removeObserver(obs);}
	
	/** SelectionEngineObservers trigger as the selection that's being built
	 * changes and when the Built Selection has changed. */
	public static interface MSelectionEngineObserver {
		public void selectionBuilt(SelectionEvent evt);
		public void buildingSelection( SelectionEvent evt);
	}
	public static class SelectionEvent {
		//Selection selection;	// TODO
	}
	
    void triggerSelectionChanged(SelectionEvent evt) {
    	selectionObs.trigger((MSelectionEngineObserver obs) -> {obs.selectionBuilt(evt);});
    }
    void triggerBuildingSelection(SelectionEvent evt) {
    	selectionObs.trigger((MSelectionEngineObserver obs) -> {obs.buildingSelection(evt);});
    }

}
