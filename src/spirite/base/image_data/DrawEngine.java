package spirite.base.image_data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import spirite.base.brains.MasterControl;
import spirite.base.brains.SettingsManager;
import spirite.base.brains.ToolsetManager.ColorChangeScopes;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.SelectionEngine.BuiltSelection;
import spirite.base.image_data.UndoEngine.ImageAction;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.image_data.images.IBuiltImageData;
import spirite.base.image_data.layers.Layer;
import spirite.base.pen.PenTraits;
import spirite.base.pen.PenTraits.PenDynamics;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.StrokeEngine;
import spirite.base.pen.StrokeEngine.STATE;
import spirite.base.util.Colors;
import spirite.base.util.MUtil;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Vec2;
import spirite.base.util.glmath.Vec2i;
import spirite.hybrid.DirectDrawer;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;
import spirite.hybrid.MDebug.WarningType;
//import spirite.pc.graphics.ImageBI;

/***
 * Pretty much anything which alters the image data directly goes 
 * through the DrawEngine.
 * 
 * @author Rory Burks
 *
 */
public class DrawEngine {
	private final ImageWorkspace workspace;
	private final UndoEngine undoEngine;
	private final SelectionEngine selectionEngine;
	private final SettingsManager settingsManager;
	private StrokeEngine activeEngine = null;
	
	public DrawEngine( ImageWorkspace workspace, MasterControl master) {
		this.workspace = workspace;
		this.undoEngine = workspace.getUndoEngine();
		this.selectionEngine = workspace.getSelectionEngine();
		this.settingsManager = master.getSettingsManager();
		
	}
	
	public boolean strokeIsDrawing() {return activeEngine != null;}
	public StrokeEngine getStrokeEngine() { return activeEngine; }
	
	private BuildingImageData workingData;
	/** Starts a Stroke with the provided parameters
	 * 
	 * @return true if the stroke started, false otherwise	 */
	public boolean startStroke(StrokeEngine.StrokeParams stroke, PenState ps, BuildingImageData data) {
		workingData = data;
		if( activeEngine != null) {
			MDebug.handleError(ErrorType.STRUCTURAL, "Tried to draw two strokes at once within the DrawEngine (if you need to do that, manually instantiate a separate StrokeEngine.");
			return false;
		}
		else if( data == null) {
			MDebug.handleError(ErrorType.STRUCTURAL, "Tried to start stroke on null data.");
			return false;
		}
		else {
			activeEngine = settingsManager.getDefaultDrawer().getStrokeEngine();
			
			if( activeEngine.startStroke(stroke, ps, workspace.buildData(data), pollSelectionMask()))
				data.handle.refresh();
			return true;
		}
	}
	
	/** Updates the active stroke. */
	public void stepStroke( PenState ps) {
		if( activeEngine == null || activeEngine.getState() != STATE.DRAWING) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Tried to step stroke that isn't active.");
			return ;
		}
		else {
			if(activeEngine.stepStroke(ps))
				activeEngine.getImageData().handle.refresh();
		}
	}
	/** Ends the active stroke. */
	public void endStroke( ) {
		if( activeEngine == null || activeEngine.getState() != STATE.DRAWING) {
			activeEngine = null;
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Tried to end stroke that isn't active.");
			return ;
		}
		else {
			activeEngine.endStroke();
				
			undoEngine.storeAction(
				new StrokeAction(
					activeEngine,
					activeEngine.getParams(),
					activeEngine.getHistory(),
					activeEngine.getLastSelection(),
					workingData));
			activeEngine = null;
		}
		
	}
	

	// ==============
	// ==== Stroke Dynamics
	// TODO: This should probably be in some settings area
	
	public static PenDynamics getBasicDynamics() {
		return basicDynamics;
	}
	private static final PenDynamics basicDynamics = new PenDynamics() {
		@Override
		public float getSize(PenState ps) {
			return ps.pressure;
		}
	};

	private static final PenDynamics personalDynamics = new PenTraits.LegrangeDynamics(
		Arrays.asList( new Vec2[] {
				new Vec2(0,0),
				new Vec2(0.25f,0),
				new Vec2(1,1)
			}
		)
	);
	
	public static PenDynamics getDefaultDynamics() {
		return personalDynamics;
	}
	private static final PenDynamics defaultDynamics = new PenDynamics() {
		@Override
		public float getSize(PenState ps) {
			return ps.pressure;
		}
	};

	private void execute( MaskedImageAction action) {
		undoEngine.performAndStore(action);
	}

	// ===============
	// ==== Queued Selection
	// Because many drawing actions can filter based on Selection
	// Mask, when re-doing them the mask which was active at the time
	// has to be remembered.  This function will apply the selection mask
	// to the next draw action performed.  If there is no seletion mask
	// queued, it will use the active selection.
	
	private void queueSelectionMask( BuiltSelection mask) {
		queuedSelection = mask;
	}
	private BuiltSelection pollSelectionMask() {
		if( queuedSelection == null)
			return workspace.getSelectionEngine().getBuiltSelection();

		BuiltSelection ret = queuedSelection;
		queuedSelection = null;
		return ret;
	}
	private BuiltSelection queuedSelection = null;
	
	// ==================
	// ==== UndoableActions
	//	Note: All actions classes are public so things that peak at the UndoEngine
	//	can know exactly what actions were performed, but all Constructors are
	//	(effectively) private so that they can only be created by the DrawEngine
	
	public abstract class MaskedImageAction extends ImageAction {
		protected final BuiltSelection mask;

		private MaskedImageAction(BuildingImageData data, BuiltSelection mask) {
			super(data);
			this.mask = mask;
		}
	}
	
	public class StrokeAction extends MaskedImageAction {
		private final PenState[] points;
		private final StrokeEngine.StrokeParams params;
		private final StrokeEngine engine;
		
		StrokeAction( 
				StrokeEngine engine,
				StrokeEngine.StrokeParams params, 
				PenState[] points, 
				BuiltSelection mask, 
				BuildingImageData data)
		{
			super(data, mask);
			this.engine = engine;
			this.params = params;
			this.points = points;
			
			switch( params.getMethod()) {
			case BASIC:
				description = "Basic Stroke Action";
				break;
			case ERASE:
				description = "Erase Stroke Action";
				break;
			case PIXEL:
				description = "Pixel Stroke Action";
				break;
			}
		}
		
		public StrokeEngine.StrokeParams getParams() {
			return params;
		}
		
		@Override
		public void performImageAction( ) {
			queueSelectionMask(mask);
			
			IBuiltImageData built = workspace.buildData(builtImage);
			engine.batchDraw(params, points, built, mask);
		}
	}
}
