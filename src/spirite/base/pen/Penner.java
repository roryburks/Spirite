package spirite.base.pen;

import java.awt.BasicStroke;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import spirite.base.brains.MasterControl;
import spirite.base.brains.PaletteManager;
import spirite.base.brains.RenderEngine;
import spirite.base.brains.SettingsManager;
import spirite.base.brains.ToolsetManager;
import spirite.base.brains.RenderEngine.RenderSettings;
import spirite.base.brains.ToolsetManager.MToolsetObserver;
import spirite.base.brains.ToolsetManager.Tool;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.image_data.DrawEngine;
import spirite.base.image_data.GroupTree;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.SelectionEngine;
import spirite.base.image_data.UndoEngine;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace.BuiltImageData;
import spirite.base.image_data.RawImage;
import spirite.base.image_data.SelectionEngine.BuildMode;
import spirite.base.image_data.SelectionEngine.BuiltSelection;
import spirite.base.image_data.SelectionEngine.FreeformSelectionBuilder;
import spirite.base.image_data.SelectionEngine.Selection;
import spirite.base.image_data.SelectionEngine.SelectionBuilder;
import spirite.base.image_data.layers.SpriteLayer;
import spirite.base.image_data.layers.SpriteLayer.Part;
import spirite.base.pen.PenTraits.ButtonType;
import spirite.base.pen.PenTraits.MButtonEvent;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.StrokeEngine.StrokeParams.InterpolationMethod;
import spirite.base.util.Colors;
import spirite.base.util.MUtil;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.MatTrans.NoninvertableException;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2;
import spirite.base.util.glmath.Vec2i;
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
	implements KeyEventDispatcher, ActionListener, MToolsetObserver
{
	// Contains "Image to Screen" and "Screen to Image" methods.
	//	Could possibly wrap them in an interface to avoid tempting Penner 
	//	with UI controls
	private final WorkPanel context;
	private View view;	
	
	
	private ImageWorkspace workspace;
	private SelectionEngine selectionEngine;
	private UndoEngine undoEngine;
	private DrawEngine drawEngine;
	private final ToolsetManager toolsetManager;
	private final PaletteManager paletteManager;
	private final SettingsManager settingsManager;
	private final RenderEngine renderEngine;	// used for color picking.
												// might not belong here, maybe in DrawEngine
	

	private boolean holdingShift = false;
	private boolean holdingCtrl = false;
	private boolean holdingAlt = false;
	
	// Naturally, being a mouse (or drawing tablet)-based input handler with
	//	a variety of states, a large number of coordinate sets need to be 
	//	remembered.  While it's possible that some of these could be condensed
	//	into fewer variables, it wouldn't be worth it just to save a few bytes
	//	of RAM
	//private int startX;	// Set at the start 
//	private int startY;
	private int oldX;	// OldX and OldY are the last-checked X and Y primarily used
	private int oldY;	// 	for things that only happen if they change
	private int rawX;	// raw position are the last-recorded coordinates in pure form
	private int rawY;	// 	(screen coordinates relative to the component Penner watches over)
	private int oldRawX;	// Similar to oldX and oldY but for 
	private int oldRawY;
	
	
	private float pressure = 1.0f;
	
	private int x, y;

	
	private StateBehavior behavior;
	
	public Penner( WorkPanel context, MasterControl master) {
		this.view = context.getCurrentView();
		this.context = context;
		this.toolsetManager = master.getToolsetManager();
		this.paletteManager = master.getPaletteManager();
		this.renderEngine = master.getRenderEngine();
		this.settingsManager = master.getSettingsManager();
		
		toolsetManager.addToolsetObserver(this);
		
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.addKeyEventDispatcher(this);
	}
	
	// ==========
	// ==== Workspace Management
	public void changeWorkspace( ImageWorkspace newWorkspace, View view) {
		this.cleanseState();

		workspace = newWorkspace;
		if( workspace != null) {
			selectionEngine = workspace.getSelectionEngine();
			undoEngine = workspace.getUndoEngine();
			drawEngine = workspace.getDrawEngine();
		}
		else {
			selectionEngine = null;
			undoEngine = null;
			drawEngine = null;
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
				behavior = new ZoomingReference();
				behavior.start();
			}
			else if( holdingShift) {
				behavior = new RotatingReference();
				behavior.start();
			}
			else {
				behavior = new GlobalRefMove();
				behavior.start();
			}
		}
		else {
			// Tool-based State-starting
			Tool tool = toolsetManager.getSelectedTool();
			
			switch( tool) {
			case PEN:
				if( holdingCtrl) 
					behavior = new PickBehavior( mbe.buttonType == ButtonType.LEFT);
				else 
					behavior = new PenBehavior((mbe.buttonType == ButtonType.LEFT) ? 
									paletteManager.getActiveColor(0)
									: paletteManager.getActiveColor(1));
				break;
			case ERASER:
				behavior = new EraseBehavior();
				break;
			case FILL:
				fill( mbe.buttonType == ButtonType.LEFT);
				break;
			case BOX_SELECTION: {
				Selection selection = selectionEngine.getSelection();
				
				if( selection != null &&  !holdingShift && !holdingCtrl &&
						selection.contains(x-selectionEngine.getOffsetX(),y-selectionEngine.getOffsetY())) 
				{
					behavior = new MovingSelectionBehavior();
				}
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
					behavior = new FormingSelectionBehavior((Integer)settings.getValue("shape"), mode);
				}
				break;}
			case FREEFORM_SELECTION: {
				Selection selection = selectionEngine.getSelection();
				
				if( selection != null && !holdingShift && !holdingCtrl &&
						selection.contains(x-selectionEngine.getOffsetX(),y-selectionEngine.getOffsetY())) 
				{
					behavior = new MovingSelectionBehavior();
				}
				else  {
					BuildMode mode;
					if( holdingShift && holdingCtrl)
						mode = BuildMode.INTERSECTION;
					else if( holdingShift)
						mode = BuildMode.ADD;
					else if( holdingCtrl)
						mode = BuildMode.SUBTRACT;
					else mode = BuildMode.DEFAULT;
					behavior = new FreeFormingSelectionBehavior(mode);
				}
				break;}
			case MOVE:{
				Selection selection = selectionEngine.getSelection();
				
				if(selection != null)
					behavior = new MovingSelectionBehavior();
				else if(workspace.getSelectedNode() != null) 
					behavior = new MovingNodeBehavior(workspace.getSelectedNode());

				break;}
			case COLOR_PICKER:
				behavior = new PickBehavior(mbe.buttonType == ButtonType.LEFT);
				break;
			case PIXEL:
				if( holdingCtrl)  {
					behavior = new PickBehavior(mbe.buttonType == ButtonType.LEFT);
				}
				else {
					behavior = new PixelBehavior((mbe.buttonType == ButtonType.LEFT) ? 
							paletteManager.getActiveColor(0)
							: paletteManager.getActiveColor(1));
				}
				break;
			case CROP:
				behavior = new CroppingBehavior();
				break;
			case COMPOSER:
				Node node = workspace.getSelectedNode();
				if( !(node instanceof LayerNode)
					|| (!(((LayerNode)node).getLayer() instanceof SpriteLayer))) 
					break;
				
				SpriteLayer rig = (SpriteLayer)(((LayerNode)workspace.getSelectedNode()).getLayer());
				SpriteLayer.Part part = rig.grabPart(x-node.getOffsetX(), y-node.getOffsetY(), true);
				
				if( part == null) part = rig.getActivePart();
				
				if( holdingShift)
					behavior = new MovingRigPart(rig, part);
				
				break;
			case FLIPPER:{
				ToolSettings settings = toolsetManager.getToolSettings(Tool.FLIPPER);
				Integer flipMode = (Integer)settings.getValue("flipMode");
				
				switch( flipMode) {
				case 0:	// Horizontal
					drawEngine.flip( workspace.buildActiveData(), true);
					break;
				case 1:	// Vertical
					drawEngine.flip( workspace.buildActiveData(), false);
					break;
				case 2:
					behavior = new FlippingBehavior();
					break;
				}
				break;}
			case RESHAPER:{
				BuiltSelection sel =selectionEngine.getBuiltSelection();
				
				if( sel == null || sel.selection == null) {
					selectionEngine.setSelection( selectionEngine.buildRectSelection(
							new Rect(0,0,workspace.getWidth(),workspace.getHeight())));
				}
				if( !selectionEngine.isLifted())
					selectionEngine.liftData();
				if( mbe.buttonType == ButtonType.LEFT) {
					behavior = new ReshapingBehavior();
/*					UndoableAction ra = workspace.getUndoEngine().createReplaceAction(
							workspace.buildActiveData().handle, 
							drawEngine.scale(workspace.buildActiveData().handle.deepAccess()));
					workspace.getUndoEngine().performAndStore(ra);*/
				}
				else {
				}
				break;}
			case COLOR_CHANGE: {
				if( holdingCtrl)  {
					behavior = new PickBehavior(mbe.buttonType == ButtonType.LEFT);
					break;
				}
				ToolSettings settings = toolsetManager.getToolSettings(Tool.COLOR_CHANGE);
				
				int scope = (Integer)settings.getValue("scope");
				int mode = (Integer)settings.getValue("mode");
				
				if( mbe.buttonType == ButtonType.LEFT) {
					drawEngine.changeColor(
							paletteManager.getActiveColor(0),
							paletteManager.getActiveColor(1),
							scope, mode);
				}
				else {
					drawEngine.changeColor(
							paletteManager.getActiveColor(1),
							paletteManager.getActiveColor(0),
							scope, mode);
				}
				break;}
			}
			
			if( behavior != null)
				behavior.start();
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
		BuiltImageData data = workspace.buildActiveData();
		GroupTree.Node node = workspace.getSelectedNode();
		
		if( data != null && node != null) {
			// Perform the fill Action, only store the UndoAction if 
			//	an actual change is made.
			drawEngine.fill( x, y, c, data);
		} 
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
	
	// By design, StateBehavior has and should make use of all local variables
	//	relevant to it, variables passed to it (if any) are for convenience only
	//	as the StateBehavior could have either accessed them or caculated them
	//	itself.
	abstract class StateBehavior {
		public abstract void start();
		public abstract void onTock();
		public abstract void onMove();
		
		// For most StateBehavior, onPenDown will be irrelevant/not make sense
		//	because their penUp action is to cancel the state.
		public void onPenDown() {}
		public void onPenUp() {
			end();
		}
		
		public void end() {
			// This effectively ends the state behavior
			behavior = null;
		}
	}
	
	abstract class DrawnStateBehavior extends StateBehavior {
		@Override
		public void end() {
			super.end();
			context.repaint();
		}
		public abstract void paintOverlay( GraphicsContext gc);
	}
	
	abstract class StrokeBehavior extends StateBehavior {
		int shiftX = rawX;
		int shiftY = rawY;
		int dx = x;
		int dy = y;
		private int shiftMode = -1;	// 0 : accept any, 1 : horizontal, 2: vertical
		
		public void startStroke (StrokeEngine.StrokeParams stroke) {
			if( workspace != null && workspace.buildActiveData() != null) {
				shiftX = rawX;
				shiftY = rawY;
				BuiltImageData data = workspace.buildActiveData();
//				GroupTree.Node node = workspace.getSelectedNode();
				
				if( !drawEngine.startStroke(stroke, new PenState(x,y,pressure), data))
					end();
			}
		}
		
		@Override
		public void onTock() {
			if( holdingShift) {
				if( shiftMode == -1) {
					shiftMode = 0;
					shiftX = rawX;
					shiftY = rawY;
				}
				if( shiftMode == 0) {
					if( Math.abs(shiftX - rawX) > 10)
						shiftMode = 1;
					else if( Math.abs(shiftY - rawY) > 10)
						shiftMode = 2;
				}
				
				if( shiftMode == 1)
					dx = x;
				if( shiftMode == 2)
					dy = y;
			}
			else {
				shiftMode = -1;
				dx = x;
				dy = y;
			}
			drawEngine.stepStroke( new PenState( dx, dy, pressure));

		}

		@Override
		public void onPenUp() {
			drawEngine.endStroke();
			super.onPenUp();
		}
		
		@Override public void onMove() {}
	}
	
	class EraseBehavior extends StrokeBehavior {
		@Override
		public void start() {
			ToolSettings settings = toolsetManager.getToolSettings(Tool.ERASER);
			StrokeEngine.StrokeParams stroke = new StrokeEngine.StrokeParams();
			stroke.setMethod( StrokeEngine.Method.ERASE);
			stroke.setWidth((float)settings.getValue("width"));
			stroke.setHard((Boolean)settings.getValue("hard"));

			// Start the Stroke
			startStroke( stroke);
		}
	}
	

	class PenBehavior extends StrokeBehavior {
		final int color;
		PenBehavior( int i) {
			this.color = i;
		}
		@Override
		public void start() {
			ToolSettings settings = toolsetManager.getToolSettings(Tool.PEN);
			StrokeEngine.StrokeParams stroke = new StrokeEngine.StrokeParams();
			stroke.setColor( color);
			stroke.setWidth((float)settings.getValue("width"));
			stroke.setAlpha((float)settings.getValue("alpha"));
			stroke.setHard((Boolean)settings.getValue("hard"));
			
			// Start the Stroke
			startStroke( stroke);
		}
	}
	class PixelBehavior extends StrokeBehavior {
		final int color;
		PixelBehavior( int i) {
			this.color = i;
		}
		@Override
		public void start() {
			ToolSettings settings = toolsetManager.getToolSettings(Tool.PIXEL);
			StrokeEngine.StrokeParams stroke = new StrokeEngine.StrokeParams();
			stroke.setMethod( StrokeEngine.Method.PIXEL);
			stroke.setAlpha((float)settings.getValue("alpha"));
			stroke.setHard(true);
			stroke.setColor( color);
			stroke.setInterpolationMethod(InterpolationMethod.NONE);
			startStroke( stroke);
		}
	}
	
	class PickBehavior extends StateBehavior {
		final boolean leftClick;
		
		PickBehavior( boolean leftClick) {
			this.leftClick = leftClick;
		}
		
		private void pickColor() {
			// Get the composed image
			RenderSettings settings = new RenderSettings(
					renderEngine.getDefaultRenderTarget(workspace));
			RawImage img = renderEngine.renderImage(settings);
			
			if( !MUtil.coordInImage(x, y, img))
				return;
			paletteManager.setActiveColor( (leftClick)?0:1, img.getRGB(x, y));
		}

		@Override
		public void start() {
			pickColor();
		}
		@Override
		public void onMove() {
			pickColor();
		}
		@Override public void onTock() {}
	}
	
	class MovingNodeBehavior extends StateBehavior {
		final Node node;
		
		MovingNodeBehavior( Node node) {
			this.node = node;
		}
		@Override public void start() {}
		@Override public void onTock() {}
		@Override
		public void onMove() {
			if( node != null && (oldX != x || oldY != y))
				node.setOffset( node.getOffsetX() + (x - oldX), 
								 node.getOffsetY() + (y - oldY));
		}
	}
	class MovingSelectionBehavior extends StateBehavior {
		@Override public void start() {}
		@Override public void onTock() {}
		@Override
		public void onMove() {
			if( oldX != x || oldY != y) 
				selectionEngine.setOffset(
						selectionEngine.getOffsetX() + (x - oldX),
						selectionEngine.getOffsetY() + (y - oldY));
		}
	}
	class MovingRigPart extends StateBehavior {
		private final SpriteLayer rig;
		private final Part part;
		MovingRigPart( SpriteLayer rig, Part part) {
			this.rig = rig;
			this.part = part;
		}

		@Override public void start() {}
		@Override public void onTock() {}
		@Override
		public void onMove() {
			undoEngine.performAndStore(
					rig.createModifyPartAction( part, 
							part.getOffsetX() + (x - oldX),
							part.getOffsetY() + (y - oldY), 
							part.getDepth(), 
							part.getTypeName(), 
							part.isVisible(), 
							part.getAlpha())
				);
		}
	}
	class ZoomingReference extends StateBehavior {
		int startx = x;
		int starty = y;
		@Override public void start() {}
		@Override public void onTock() {}
		@Override
		public void onMove() {
			workspace.getReferenceManager().zoomTransform(
					(float)Math.pow(1.0015, 1+(rawY - oldRawY)), startx, starty);
		}
	}
	class RotatingReference extends StateBehavior {
		int startx = x;
		int starty = y;
		int ox = rawX;
		@Override public void start() {}
		@Override public void onTock() {}
		@Override
		public void onMove() {
			float theta = (rawX-ox)*0.05f;
			ox = rawX;
			workspace.getReferenceManager().rotateTransform(theta, startx, starty);
		}
	}
	class GlobalRefMove extends StateBehavior {
		@Override public void start() {}
		@Override public void onTock() {}
		@Override
		public void onMove() {
			workspace.getReferenceManager().shiftTransform((x-oldX), (y-oldY));
		}
	}
	
	class FormingSelectionBehavior extends StateBehavior {
		private final int shape;
		private final BuildMode mode;
		FormingSelectionBehavior( int shape, BuildMode mode) {
			this.shape = shape;
			this.mode = mode;
		}
		@Override
		public void start() {
			SelectionBuilder builder = null;
			
			switch( shape) {
			case 0:
				builder = selectionEngine.new RectSelectionBuilder();
				break;
			case 1:
				builder = selectionEngine.new OvalSelectionBuilder();
				break;
			}
			
			selectionEngine.startBuildingSelection( builder, x, y, mode);
		}
		@Override
		public void onMove() {
			selectionEngine.updateBuildingSelection(x, y);
		}
		@Override
		public void onPenUp() {
			selectionEngine.finishBuildingSelection();
			super.onPenUp();
		}
		@Override
		public void onTock() {
//			selectionEngine.updateBuildingSelection(x, y);
		}
	}

	class FreeFormingSelectionBehavior extends DrawnStateBehavior {
		private boolean drawing = true;
		private final BuildMode mode;
		private FreeformSelectionBuilder builder;
		FreeFormingSelectionBehavior( BuildMode mode) {
			this.mode = mode;
		}
		@Override
		public void start() {
			builder = selectionEngine.new FreeformSelectionBuilder();
			selectionEngine.startBuildingSelection( builder, x, y, mode);
		}

		@Override
		public void onMove() {
			if( drawing && (x != oldX || y != oldY))
				selectionEngine.updateBuildingSelection(x, y);
		}
		@Override public void onTock() {}
		public boolean testFinish() {
			Vec2i p_s = builder.getStart();
			if( MUtil.distance(p_s.x, p_s.y, x, y)<=5) {
				selectionEngine.finishBuildingSelection();
				this.end();
				return true;
			}
			return false;
		}
		@Override public void onPenUp() {
			drawing = false;
			testFinish();
		}
		@Override
		public void onPenDown() {
			drawing = true;
			if( !testFinish())
				selectionEngine.updateBuildingSelection(x, y);
		}
		@Override
		public void paintOverlay(GraphicsContext g) {
			if( !drawing) {
				Vec2i p_e = builder.getEnd();
				
				g.setColor( Colors.BLACK);
				g.drawLine(view.itsXm(p_e.x), view.itsYm(p_e.y), 
						view.itsXm(x), view.itsYm(y));

			}

			Vec2i p_s = builder.getStart();
			if( MUtil.distance(p_s.x, p_s.y, x, y)<=5) {
				g.setColor( Colors.YELLOW);
				g.fillOval(view.itsXm(p_s.x)-5, view.itsYm(p_s.y) - 5, 10, 10);
			}
			else
				g.drawOval(view.itsXm(p_s.x)-5, view.itsYm(p_s.y) - 5, 10, 10);
		}
	}
	class CroppingBehavior extends DrawnStateBehavior {
		boolean building = false;
		boolean modifying = false;
		Rect cropSection = null;
		Rect middle;
		Rect topRight;
		Rect topLeft;
		Rect bottomRight;
		Rect bottomLeft;
		int startx, starty;
		//	0x1 : Top
		//	0x2 : Bottom
		//	0x4 : Left
		//	0x8 : Right
		byte cardinalMap = 0x00;

		static final byte TOPMASK = 0x01;
		static final byte BOTTOMMASK = 0x02;
		static final byte LEFTMASK = 0x04;
		static final byte RIGHTMASK = 0x08;
//		Rectangle 
		
		private void buildCrop( ) {
			middle = MUtil.scaleRect( cropSection, 0.6f);
			topLeft = MUtil.scaleRect( cropSection, 0.2f);
			topLeft.x = cropSection.x;
			topLeft.y = cropSection.y;
			topRight = new Rect(topLeft);
			topRight.x = cropSection.x + cropSection.width - topRight.width;
			topRight.y = cropSection.y;
			bottomLeft = new Rect(topLeft);
			bottomLeft.x = cropSection.x;
			bottomLeft.y = cropSection.y + cropSection.height - bottomLeft.height;
			bottomRight = new Rect(topLeft);
			bottomRight.x = cropSection.x + cropSection.width - bottomRight.width;
			bottomRight.y = cropSection.y + cropSection.height - bottomRight.height;
		}

		@Override
		public void start() {
			building = true;
			startx = x;
			starty = y;
			cropSection = new Rect( x, y, 0, 0);
		}

		@Override
		public void onPenUp() {
	
			ToolSettings settings = toolsetManager.getToolSettings(Tool.CROP);
	
			cardinalMap = 0;


			if( building) {
				cropSection = MUtil.rectFromEndpoints(
						startx, starty, x, y);
				if( (Boolean)settings.getValue("quickCrop")) {
					workspace.cropNode(
						workspace.getSelectedNode(), 
						cropSection,
						(Boolean)settings.getValue("shrinkOnly"));
					end();
				}
				else
					buildCrop();
				
				building = false;
			}
		}
		
		@Override public void onTock() {}

		@Override
		public void onMove() {

			if( building) {
				cropSection = MUtil.rectFromEndpoints(
						startx, starty, x, y);
			}
			else if( modifying) {
				if( (cardinalMap & TOPMASK) != 0 ) {
					cropSection.y += y - oldY;
					cropSection.height -= (y - oldY);
				}
				if( (cardinalMap & BOTTOMMASK) != 0) {
					cropSection.height += (y - oldY);
				}
				if( (cardinalMap & LEFTMASK) != 0) {
					cropSection.x += (x - oldX);
					cropSection.width -= (x - oldX);
				}
				if( (cardinalMap & RIGHTMASK)!= 0) {
					cropSection.width+= (x - oldX);
				}
				buildCrop();
			}
		}
		
		@Override
		public void onPenDown() {
			if( toolsetManager.getSelectedTool() != Tool.CROP) { end(); return;}
			
			if( cropSection == null || !cropSection.contains(x, y)) {
				building = true;
				startx = x;
				starty = y;
			}
			else {
				cardinalMap = 0;
				
				if( middle.contains( x, y)) {
					workspace.cropNode(
							workspace.getSelectedNode(), 
							cropSection,
							(Boolean)toolsetManager.getToolSettings(Tool.CROP).getValue("shrinkOnly"));
					
					end();
				}
				else if( topRight.contains(x, y)) 
					cardinalMap = TOPMASK | RIGHTMASK;
				else if( topLeft.contains(x,y))
					cardinalMap = TOPMASK | LEFTMASK;
				else if( bottomLeft.contains(x,y))
					cardinalMap = BOTTOMMASK| LEFTMASK;
				else if( bottomRight.contains(x,y))
					cardinalMap = BOTTOMMASK | RIGHTMASK;
				
				if( cardinalMap != 0)
					modifying = true;
			}
		}

		@Override
		public void paintOverlay(GraphicsContext gc) {
			
			// Outline
			// TODO:
/*            Stroke new_stroke = new BasicStroke(
            		1, 
            		BasicStroke.CAP_BUTT, 
            		BasicStroke.JOIN_BEVEL, 
            		0, 
            		new float[]{8,4}, 0);
            g2.setStroke(new_stroke);*/
            
            View view = context.getCurrentView();
            Rect r = view.itsRm(cropSection);
			gc.setColor(Colors.BLACK);
            gc.drawRect(r.x, r.y, r.width, r.height);
			

            // Grey area outside
//			Composite c = gc.getComposite();
			int x1 = view.itsXm(0);
			int y1 = view.itsYm(0);
			int x2 = view.itsXm(workspace.getWidth());
			int y2 = view.itsYm(workspace.getHeight());

			if( r.x < x1) { r.width -= x1 - r.x; r.x = x1;}
			if( r.x + r.width > x2) { r.width = x2 - r.x;}

			gc.setColor(Colors.YELLOW);
			gc.setComposite(Composite.SRC_OVER, 0.4f);
			gc.fillRect( x1, y1, r.x - x1 - 1, y2-y1);
			gc.fillRect( r.x-1, y1, r.width+2, r.y - y1 - 1);
			gc.fillRect( r.x-1, r.y + r.height+1, r.width+2, y2 - (r.height+ r.y) + 1);
			gc.fillRect( r.x + r.width+1,  y1, x2 - (r.width+r.x)+1, y2-y1);
			
			// The various inner rectangles represenging the modification points
			if( !building) {
//				gc.setStroke(new BasicStroke(2.0f));
				
				if( middle.contains(x,y)) {
					r = view.itsRm( middle);
					gc.setColor(Colors.YELLOW);
		            gc.drawRect(r.x, r.y, r.width, r.height);
				}

				if( topRight.contains(x,y))
					gc.setColor(Colors.YELLOW);
				else
					gc.setColor(Colors.WHITE);
				r = view.itsRm(topRight);
	            gc.drawRect(r.x, r.y, r.width, r.height);

				if( topLeft.contains(x,y))
					gc.setColor(Colors.YELLOW);
				else
					gc.setColor(Colors.WHITE);
				r = view.itsRm(topLeft);
	            gc.drawRect(r.x, r.y, r.width, r.height);

				if( bottomLeft.contains(x,y))
					gc.setColor(Colors.YELLOW);
				else
					gc.setColor(Colors.WHITE);
				r = view.itsRm(bottomLeft);
	            gc.drawRect(r.x, r.y, r.width, r.height);

				if( bottomRight.contains(x,y))
					gc.setColor(Colors.YELLOW);
				else
					gc.setColor(Colors.WHITE);
				r = view.itsRm(bottomRight);
	            gc.drawRect(r.x, r.y, r.width, r.height);
			}

			gc.setComposite(Composite.SRC_OVER, 1.0f);
//    		gc.setComposite(c);
//    		gc.setStroke(s);
		}

	}
	

	enum ReshapeStates {
		READY, ROTATE, RESIZE, MOVING
	}
	class ReshapingBehavior extends DrawnStateBehavior {
		// The Working Transform is the transform which is used for drawing
		MatTrans wTrans = new MatTrans();
		
		// The Lock Transform is the transform stored at the start of the current
		//	action, which is used in combination of the transformation of the current
		//	action to create the Working Transform
		MatTrans lockTrans = new MatTrans();
		
		// The Calculation Transform is a version of the Locked Transform which has
		//	all the relevent offsets built-in so that calculation changes in mouse
		//	movement with respect to the selection's center can be easily performed.
		MatTrans calcTrans = new MatTrans();
		ReshapeStates state = ReshapeStates.READY;
		int startX,startY;
		int overlap = -1;
		// 0123 : NESW
		// 4567 : NW NE SE SW
		// 89AB : NW NE SE SW (rotation)
		// C : Moving
		
		@Override
		public void paintOverlay(GraphicsContext gc) {
			float zoom = view.getZoom();
			
			BuiltSelection sel =selectionEngine.getBuiltSelection();
			
			if( sel == null || sel.selection == null){
				this.end();
				return;
			}
			Vec2i d = sel.selection.getDimension();
			
			MatTrans origTrans = gc.getTransform();

			MatTrans relTrans = new MatTrans();
			relTrans.translate(view.itsX(0), view.itsY(0));
			relTrans.scale(zoom, zoom);
			relTrans.translate(d.x/2+sel.offsetX, d.y/2+sel.offsetY);
			relTrans.concatenate(wTrans);
			relTrans.translate(-d.x/2, -d.y/2);
			
			gc.setTransform(relTrans);

			gc.setColor(Colors.BLACK);
			gc.drawRect( 0, 0, d.x, d.y);
			
			Stroke defStroke = new BasicStroke( 2/zoom);
			gc.setColor(Colors.GRAY);
//			gc.setStroke(defStroke);
			
			Vec2 p = new Vec2();
			try {
				p = relTrans.inverseTransform(new Vec2(rawX,rawY), p);
			} catch (NoninvertableException e) {
				e.printStackTrace();
			}
			
			float sw = d.x*0.3f;	// Width of corner rect
			float sh = d.y*0.3f;	// Height
			float x2 = d.x*0.7f;	// Offset of right rect
			float y2 = d.y*0.7f;	// " bottom
			float di = d.y*0.2f;	// Diameter of rotate thing
			float of = d.y*0.25f*0.2f;

			float b = 4/zoom;
			
			List<Shape> s = new ArrayList<>(12);
			s.add(new Rectangle2D.Float(sw+b, b, x2-sw-b*2, sh-b*2));	// N
			s.add(new Rectangle2D.Float(x2+b, sh+b, sw-b*2, y2-sh-b*2));// E
			s.add(new Rectangle2D.Float(sw+b, y2+b, x2-sw-b*2, sh-b*2));// S
			s.add(new Rectangle2D.Float(0+b, sh+b, sw-b*2, y2-sh-b*2));	// W
			
			s.add(new Rectangle2D.Float(b, b, sw-b*2, sh-b*2));			// NW
			s.add(new Rectangle2D.Float(x2+b, b, sw-b*2, sh-b*2));		// NE
			s.add(new Rectangle2D.Float(x2+b, y2+b, sw-b*2, sh-b*2));	// SE
			s.add(new Rectangle2D.Float(b, y2+b, sw-b*2, sh-b*2));		// SW

			s.add(new Ellipse2D.Float( -di+of, -di+of, di, di));	// NW
			s.add(new Ellipse2D.Float( d.x-of, -di+of, di, di));	// NE
			s.add(new Ellipse2D.Float( d.x-of, d.y-of, di, di));	// SE
			s.add(new Ellipse2D.Float( -di+of, d.y-of, di, di));	// SW

			s.add(new Rectangle2D.Float(sw+b, sh+b, x2-sw-b*2, y2-sh-b*2));	// Center
			
			if( this.state == ReshapeStates.READY)
				overlap = -1;
			for( int i=0; i<s.size(); ++i) {
				Shape shape = s.get(i);
				System.out.println(p.x + "," + p.y);
				if( overlap == i || (overlap == -1 && shape.contains( new Point2D.Float(p.x, p.y)))) {
					gc.setColor(Colors.YELLOW);
//					gc.setStroke(new BasicStroke( 4/zoom));
					gc.draw(shape);
					gc.setColor(Colors.GRAY);
//					gc.setStroke(defStroke);
					overlap = i;
				}
				else gc.draw(shape);
			}

			
			gc.setTransform(origTrans);
			
		}

		@Override public void onPenUp() {
			this.state = ReshapeStates.READY;
		}
		@Override
		public void onPenDown() {
			BuiltSelection sel =selectionEngine.getBuiltSelection();
			
			if( sel == null || sel.selection == null){
				this.end();
				return;
			}
			
			if( overlap >= 0 && overlap <= 7) {
				Vec2i d = sel.selection.getDimension();
				startX = x;
				startY = y;
				lockTrans = new MatTrans(wTrans);
				calcTrans = new MatTrans();
				calcTrans.translate(-sel.offsetX-d.x/2.0f, -sel.offsetY-d.y/2.0f);
				calcTrans.preConcatenate(lockTrans);
				this.state = ReshapeStates.RESIZE;
			}
			else if( overlap >= 8 && overlap <= 0xB) {
				Vec2i d = sel.selection.getDimension();
				startX = x;
				startY = y;
				lockTrans = new MatTrans(wTrans);
				calcTrans = new MatTrans();
				calcTrans.translate(-sel.offsetX-d.x/2.0f, -sel.offsetY-d.y/2.0f);
				calcTrans.preConcatenate(lockTrans);
				this.state = ReshapeStates.ROTATE;
			}
			else if( overlap == 0xC)
				this.state = ReshapeStates.MOVING;
		}
		@Override
		public void onMove() {
			BuiltSelection sel =selectionEngine.getBuiltSelection();
			
			if( sel == null || sel.selection == null){
				this.end();
				return;
			}
			
			switch( this.state) {
			case MOVING:
				if( oldX != x || oldY != y) 
					selectionEngine.setOffset(
							selectionEngine.getOffsetX() + (x - oldX),
							selectionEngine.getOffsetY() + (y - oldY));
				break;
			case READY:
				break;
			case RESIZE:{
				Vec2 pn = new Vec2();
				Vec2 ps = new Vec2();

				calcTrans.transform(new Vec2(x,y), pn);
				calcTrans.transform(new Vec2(startX,startY), ps);

				wTrans = (new MatTrans(lockTrans));
				
				wTrans.scale(pn.x/ps.x,pn.y/ps.y);
				//wTrans.concatenate(lockTrans);
				break;}
			case ROTATE:{
				Vec2 pn = new Vec2();
				Vec2 ps = new Vec2();

				calcTrans.transform(new Vec2(x,y), pn);
				calcTrans.transform(new Vec2(startX,startY), ps);
				
				wTrans = (new MatTrans(lockTrans));

				double start = Math.atan2(ps.y, ps.x);
				double end =  Math.atan2(pn.y, pn.x);
				wTrans.rotate((float)(end-start));
				break;}
			
			}
			
			selectionEngine.proposeTransform(wTrans);
		}
		@Override
		public void start() {
		}

		@Override
		public void end() {
			super.end();
			selectionEngine.stopProposintTransform();
		}
		@Override
		public void onTock() {}

	}

	class FlippingBehavior extends StateBehavior {
		int startX, startY;
		
		@Override
		public void start() {
			startX = x;
			startY = y;
		}
		@Override
		public void onMove() {
			
		}
		@Override
		public void onPenUp() {
			BuiltImageData data =  workspace.buildActiveData();
			if( data != null) {
				if( MUtil.distance(x , y, startX, startY) < 5 ||
					Math.abs(x - startX) > Math.abs(y - startY))
					drawEngine.flip( data, true);
				else
					drawEngine.flip( data, false);
			}
			
			super.onPenUp();
		}
		@Override public void onTock() {}
	}
	
	// :::: KeyEventDispatcher
	@Override
	public boolean dispatchKeyEvent(KeyEvent evt) {
		boolean shift =(( evt.getModifiers() & KeyEvent.SHIFT_MASK) != 0);
		boolean ctrl = (( evt.getModifiers() & KeyEvent.CTRL_MASK) != 0);
		boolean alt = (( evt.getModifiers() & KeyEvent.ALT_MASK) != 0);
			
		holdingShift = shift;
		holdingCtrl = ctrl;
		holdingAlt = alt;
		return false;
	}
	
	// :::: ActionListener
	@Override
	public void actionPerformed(ActionEvent evt) {
	}
	

	/** Cleans up resources that have a global-level context in Swing to avoid
	 * Memory Leaks. */
	public void cleanUp() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.removeKeyEventDispatcher(this);
		
	}

	public boolean drawsOverlay() {
		return behavior instanceof DrawnStateBehavior;
	}

	public void paintOverlay(GraphicsContext gc) {
		if( behavior instanceof DrawnStateBehavior) {
			((DrawnStateBehavior)behavior).paintOverlay(gc);
		}
	}

	// :::: MToolsetObserver
	@Override
	public void toolsetChanged(Tool newTool) {
		if(behavior != null)
			behavior.end();
		behavior = null;
		if( selectionEngine.isBuilding()) {
			selectionEngine.cancelBuildingSelection();
		}
	}

}
