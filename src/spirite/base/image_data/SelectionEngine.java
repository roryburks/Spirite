package spirite.base.image_data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.UndoEngine.ImageAction;
import spirite.base.image_data.UndoEngine.NullAction;
import spirite.base.image_data.UndoEngine.StackableAction;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.selection.ALiftedSelection;
import spirite.base.image_data.selection.SelectionMask;
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
	private ALiftedSelection lifted = null;
	
	private boolean proposingTransform = false;
	MatTrans proposedTransform = null;

	
	SelectionEngine( ImageWorkspace workspace) {
		this.workspace = workspace;
		this.undoEngine = workspace.getUndoEngine();

		workspace.triggerSelectionRefresh();
	}
	
	// ============
	// ==== Basic Gets
	public SelectionMask getSelection() {return selection;}
	public boolean isLifted() {return lifted != null;}
	public ALiftedSelection getLiftedData() {return lifted;}

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
