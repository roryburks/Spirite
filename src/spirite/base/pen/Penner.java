package spirite.base.pen;

import spirite.base.brains.MasterControl;
import spirite.base.brains.PaletteManager;
import spirite.base.brains.SettingsManager;
import spirite.base.brains.ToolsetManager;
import spirite.base.brains.ToolsetManager.BoxSelectionShape;
import spirite.base.brains.ToolsetManager.ColorChangeScopes;
import spirite.base.brains.ToolsetManager.MToolsetObserver;
import spirite.base.brains.ToolsetManager.Property;
import spirite.base.brains.ToolsetManager.Tool;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.renderer.RenderEngine;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.UndoEngine;
import spirite.base.image_data.layers.SpriteLayer;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IColorChangeModule;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IFillModule;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IFlipModule;
import spirite.base.image_data.mediums.drawer.IImageDrawer.ILiftSelectionModule;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IMagneticFillModule;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IWeightEraserModule;
import spirite.base.image_data.selection.SelectionEngine;
import spirite.base.image_data.selection.SelectionEngine.BuildMode;
import spirite.base.image_data.selection.SelectionMask;
import spirite.base.pen.PenTraits.ButtonType;
import spirite.base.pen.PenTraits.MButtonEvent;
import spirite.base.pen.behaviors.CroppingBehavior;
import spirite.base.pen.behaviors.DrawnStateBehavior;
import spirite.base.pen.behaviors.EraseBehavior;
import spirite.base.pen.behaviors.ExciseBehavior;
import spirite.base.pen.behaviors.FlippingBehavior;
import spirite.base.pen.behaviors.FormingSelectionBehavior;
import spirite.base.pen.behaviors.FreeFormingSelectionBehavior;
import spirite.base.pen.behaviors.GlobalRefMoveBehavior;
import spirite.base.pen.behaviors.MagFillingBehavior;
import spirite.base.pen.behaviors.MovingNodeBehavior;
import spirite.base.pen.behaviors.MovingSelectionBehavior;
import spirite.base.pen.behaviors.PenBehavior;
import spirite.base.pen.behaviors.PickBehavior;
import spirite.base.pen.behaviors.PixelBehavior;
import spirite.base.pen.behaviors.ReshapingBehavior;
import spirite.base.pen.behaviors.RigManipulationBehavior;
import spirite.base.pen.behaviors.RotatingReferenceBehavior;
import spirite.base.pen.behaviors.StateBehavior;
import spirite.base.pen.behaviors.ZoomingReferenceBehavior;
import spirite.base.util.MUtil;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.hybrid.HybridHelper;
import spirite.pc.ui.panel_work.WorkPanel;
import spirite.pc.ui.panel_work.WorkPanel.View;

/***
 * The Penner translates Pen and Mouse input, particularly from the draw
 * panel and then translates them into actions to be performed by the 
 * DrawEngine.
 * 
 *
 * @author Rory Burks
 */
public class Penner 
	implements MToolsetObserver
{
	// Contains "Image to Screen" and "Screen to Image" methods.
	//	Could possibly wrap them in an interface to avoid tempting Penner 
	//	with UI controls
	public final WorkPanel context;
	public View view;	
	
	
	public ImageWorkspace workspace;
	public SelectionEngine selectionEngine;
	private UndoEngine undoEngine;
	//private DrawEngine drawEngine;
	public final ToolsetManager toolsetManager;
	public final PaletteManager paletteManager;
	private final SettingsManager settingsManager;
	public final RenderEngine renderEngine;	// used for color picking.
												// might not belong here, maybe in DrawEngine
	

	public boolean holdingShift = false;
	public boolean holdingCtrl = false;
	public boolean holdingAlt = false;
	
	
	// Naturally, being a mouse (or drawing tablet)-based input handler with
	//	a variety of states, a large number of coordinate sets need to be 
	//	remembered.  While it's possible that some of these could be condensed
	//	into fewer variables, it wouldn't be worth it just to save a few bytes
	//	of RAM
	//private int startX;	// Set at the start 
//	private int startY;
	public int oldX;	// OldX and OldY are the last-checked X and Y primarily used
	public int oldY;	// 	for things that only happen if they change
	public int rawX;	// raw position are the last-recorded coordinates in pure form
	public int rawY;	// 	(screen coordinates relative to the component Penner watches over)
	public int oldRawX;	// Similar to oldX and oldY but for 
	public int oldRawY;
	
	
	public float pressure = 1.0f;
	
	public int x;
	public int y;

	
	public StateBehavior behavior;
	
	public Penner( WorkPanel context, MasterControl master) {
		this.view = context.getCurrentView();
		this.context = context;
		this.toolsetManager = master.getToolsetManager();
		this.paletteManager = master.getPaletteManager();
		this.renderEngine = master.getRenderEngine();
		this.settingsManager = master.getSettingsManager();
		
		toolsetManager.addToolsetObserver(this);
		
	}
	
	// ==========
	// ==== Workspace Management
	public void changeWorkspace( ImageWorkspace newWorkspace, View view) {
		this.cleanseState();

		workspace = newWorkspace;
		if( workspace != null) {
			selectionEngine = workspace.getSelectionEngine();
			undoEngine = workspace.getUndoEngine();
		}
		else {
			selectionEngine = null;
			undoEngine = null;
		}
		
		this.view = view;
	}
	
	// Since Penner mostly deals in Image Coordinates, not Screen Coordinates,
	//	when the Image Space changes (such as on zoom-in), the coordinates need
	//	to be updated.
	public void refreshCoordinates() {
		rawUpdateX(rawX);
		rawUpdateY(rawY);
	}
	
	public void cleanseState() {
		if( behavior != null)
			behavior.end();
		behavior = null;
		context.repaint();
	}
	
	/** Pen/Mouse input should not necessarily change the image every time
	 * a raw input is detected, because the input might stair-step, updating
	 * X and Y at different times, instead step should be called at regular
	 * short intervals (no slower than 50 times per second, preferably) to
	 * update all move behavior.*/
	public void step() {
		// Perform state-based "on-pen/mouse move" code
		if( behavior != null)

		if( (oldX != x || oldY != y) && behavior != null) {
			behavior.onMove();
			if( behavior instanceof DrawnStateBehavior)
				context.repaint();
			
			context.refreshCoordinates(x, y);
		}
		
		if( behavior != null)
			behavior.onTock();
		
		oldX = x;
		oldY = y;
		oldRawX = rawX;
		oldRawY = rawY;
		

	}
	
	
	/**
	 */
	public void penDownEvent(MButtonEvent mbe) {
		if( mbe == null) return;
		
		if( behavior != null)
			behavior.onPenDown();
		else if( workspace.getReferenceManager().isEditingReference()) {
			// Special Reference behavior
			if( holdingCtrl) {
				behavior = new ZoomingReferenceBehavior(this);
				behavior.start();
			}
			else if( holdingShift) {
				behavior = new RotatingReferenceBehavior(this);
				behavior.start();
			}
			else {
				behavior = new GlobalRefMoveBehavior(this);
				behavior.start();
			}
		}
		else if( settingsManager.getAllowsEdittingInvisible() || 
				(workspace.getSelectedNode() != null && workspace.getSelectedNode().getRender().isVisible()))
		{
				
			
			// Tool-based State-starting
			Tool tool = toolsetManager.getSelectedTool();
			
			switch( tool) {
			case PEN:
				if( holdingCtrl) 
					behavior = new PickBehavior( this, mbe.buttonType == ButtonType.LEFT);
				else 
					behavior = new PenBehavior(this, (mbe.buttonType == ButtonType.LEFT) ? 
									paletteManager.getActiveColor(0)
									: paletteManager.getActiveColor(1));
				break;
			case ERASER:
				behavior = new EraseBehavior(this);
				break;
			case FILL:
				fill( mbe.buttonType == ButtonType.LEFT);
				break;
			case BOX_SELECTION: {
				SelectionMask selection = selectionEngine.getSelection();
				
				if( selection != null &&  !holdingShift && !holdingCtrl && selection.contains(x,y)) 
					behavior = new MovingSelectionBehavior(this);
				else  {
					ToolSettings settings = toolsetManager.getToolSettings(Tool.BOX_SELECTION);

					BuildMode mode;
					if( holdingShift && holdingCtrl)
						mode = BuildMode.INTERSECTION;
					else if( holdingShift)
						mode = BuildMode.ADD;
					else if( holdingCtrl)
						mode = BuildMode.SUBTRACT;
					else mode = BuildMode.DEFAULT;
					behavior = new FormingSelectionBehavior(this, (BoxSelectionShape)settings.getValue("shape"), mode);
				}
				break;}
			case FREEFORM_SELECTION: {
				SelectionMask selection = selectionEngine.getSelection();
				
				if( selection != null && !holdingShift && !holdingCtrl && selection.contains(x,y)) 
					behavior = new MovingSelectionBehavior(this);
				else  {
					BuildMode mode;
					if( holdingShift && holdingCtrl)
						mode = BuildMode.INTERSECTION;
					else if( holdingShift)
						mode = BuildMode.ADD;
					else if( holdingCtrl)
						mode = BuildMode.SUBTRACT;
					else mode = BuildMode.DEFAULT;
					behavior = new FreeFormingSelectionBehavior(this, mode);
				}
				break;}
			case MOVE:{
				SelectionMask selection = selectionEngine.getSelection();
				
				if(selection != null)
					behavior = new MovingSelectionBehavior(this);
				else if(workspace.getSelectedNode() != null) 
					behavior = new MovingNodeBehavior(this, workspace.getSelectedNode());

				break;}
			case COLOR_PICKER:
				behavior = new PickBehavior(this, mbe.buttonType == ButtonType.LEFT);
				break;
			case PIXEL:
				if( holdingCtrl)  {
					behavior = new PickBehavior(this, mbe.buttonType == ButtonType.LEFT);
				}
				else {
					behavior = new PixelBehavior(this, (mbe.buttonType == ButtonType.LEFT) ? 
							paletteManager.getActiveColor(0)
							: paletteManager.getActiveColor(1));
				}
				break;
			case CROP:
				behavior = new CroppingBehavior(this);
				break;
			case COMPOSER:
				Node node = workspace.getSelectedNode();
				if( !(node instanceof LayerNode)
					|| (!(((LayerNode)node).getLayer() instanceof SpriteLayer))) 
					break;
				
				SpriteLayer rig = (SpriteLayer)(((LayerNode)workspace.getSelectedNode()).getLayer());
				SpriteLayer.Part part = rig.grabPart(x-node.getOffsetX(), y-node.getOffsetY(), true);				
				if( part == null) part = rig.getActivePart();
				
				behavior = new RigManipulationBehavior(this, part, node);
//				
//				if( holdingShift)
//					behavior = new MovingRigPart(rig, part);
				
				break;
			case FLIPPER:{
				ToolSettings settings = toolsetManager.getToolSettings(Tool.FLIPPER);
				Integer flipMode = (Integer)settings.getValue("flipMode");

				switch( flipMode) {
				case 0:	// Horizontal
					tryFlip(true);
					break;
				case 1:	// Vertical
					tryFlip(false);
					break;
				case 2:
					behavior = new FlippingBehavior(this);
					break;
				}
				break;}
			case RESHAPER:{
				SelectionMask sel =selectionEngine.getSelection();
				
				if( sel == null) {
					selectionEngine.setSelection( selectionEngine.buildRectSelection(
							new Rect(0,0,workspace.getWidth(),workspace.getHeight())));
				}
				if( !selectionEngine.isLifted()) {
					IImageDrawer drawer = workspace.getActiveDrawer();
					if( drawer instanceof ILiftSelectionModule)
						selectionEngine.attemptLiftData(drawer);	
					else {
						HybridHelper.beep();
						break;
					}
				}
				if( mbe.buttonType == ButtonType.LEFT) {
					behavior = new ReshapingBehavior(this);
				}
				else {
				}
				break;}
			case COLOR_CHANGE: {
				if( holdingCtrl)  {
					behavior = new PickBehavior(this, mbe.buttonType == ButtonType.LEFT);
					break;
				}
				ToolSettings settings = toolsetManager.getToolSettings(Tool.COLOR_CHANGE);
				
				ColorChangeScopes scope = (ColorChangeScopes)settings.getValue("scope");
				int mode = (Integer)settings.getValue("mode");
				
				IImageDrawer drawer = workspace.getActiveDrawer();
				
				if( drawer instanceof IColorChangeModule) {
					if( mbe.buttonType == ButtonType.LEFT) {
						((IColorChangeModule) drawer).changeColor( 
								paletteManager.getActiveColor(0),
								paletteManager.getActiveColor(1),
								scope, mode);
					}
					else {
						((IColorChangeModule) drawer).changeColor(
								paletteManager.getActiveColor(1),
								paletteManager.getActiveColor(0),
								scope, mode);
					}
				}
				else
					HybridHelper.beep();
				
				break;}
			case MAGLEV_FILL:{

				IImageDrawer drawer = workspace.getActiveDrawer();
				if( drawer instanceof IMagneticFillModule) {
					behavior = new MagFillingBehavior(this, (IMagneticFillModule) drawer);
				}
				else HybridHelper.beep();
				break;}
			case EXCISE_ERASER: {
				IImageDrawer drawer = workspace.getActiveDrawer();
				
				if( drawer instanceof IWeightEraserModule) {
					behavior = new ExciseBehavior( this, (IWeightEraserModule)drawer);
				}
				else HybridHelper.beep();
				break;}
			}
			
			if( behavior != null)
				behavior.start();
		}
		else {
			HybridHelper.beep();
		}
	}
	
	public void penUpEvent( MButtonEvent mButtonEvent)
	{
		// PenUp
		if( behavior != null) {
			behavior.onPenUp();
		}
	}

	// :::: Single-click actions that don't require StateBehavior
	private void fill( boolean leftClick) {
		// Determine Color
		int c = (leftClick) ? 
				paletteManager.getActiveColor(0)
				: paletteManager.getActiveColor(1);
				
		if( holdingCtrl) c = 0x00000000;

		// Grab the Active Data
		BuildingMediumData data = workspace.buildActiveData();
		
		IImageDrawer drawer = workspace.getDrawerFromHandle(data.handle);

		if( drawer instanceof IFillModule) {
			((IFillModule)drawer).fill(x, y, c, data);
			// Perform the fill Action, only store the UndoAction if 
			//	an actual change is made.
			//drawEngine.fill( x, y, c, data);
		} 
		else
			HybridHelper.beep();
	}

	
	// :::: Methods to feed raw data into the Penner for it to interpret.
	// !!!! Note: these methods should behave as if there's no potential behavior
	//	problem if they aren't running on the AWTEvent thread.
	public void rawUpdateX( int raw) {
		rawX = raw;
		x = view.stiXm(rawX);
	}
	public void rawUpdateY( int raw) {
		rawY = raw;
		y = view.stiYm( rawY);
	}
	public void rawUpdatePressure( float pressure) {
		this.pressure = MUtil.clip(0, (float)settingsManager.getTabletInterpolator().eval(pressure), 1);
	}
	
	public void tryFlip( boolean horizontal) {
		IImageDrawer drawer = workspace.getActiveDrawer();
		SelectionMask selected = selectionEngine.getSelection();
		
		
		if( selectionEngine.isLifted()) {
			selectionEngine.transformSelection((horizontal)
					? MatTrans.ScaleMatrix(-1, 1)
					: MatTrans.ScaleMatrix(1, -1));
		}
		else if( selected == null) {
			if( drawer instanceof IFlipModule) {
				((IFlipModule) drawer).flip(horizontal);
			}
			else HybridHelper.beep();
		}
		else {
			if( drawer instanceof ILiftSelectionModule) {
				selectionEngine.attemptLiftData(drawer);
				selectionEngine.transformSelection((horizontal)
						? MatTrans.ScaleMatrix(-1, 1)
						: MatTrans.ScaleMatrix(1, -1));
			}
			else HybridHelper.beep();
		}
	}
	
	public boolean drawsOverlay() {
		return (behavior instanceof DrawnStateBehavior);
	}

	public void paintOverlay(GraphicsContext gc) {
		if( behavior instanceof DrawnStateBehavior) {
			((DrawnStateBehavior)behavior).paintOverlay(gc);
		}
	}

	// :::: MToolsetObserver
	@Override public void toolsetPropertyChanged(Tool tool, Property property) {}
	@Override
	public void toolsetChanged(Tool newTool) {
		if(behavior != null)
			behavior.end();
		behavior = null;
	}

}
