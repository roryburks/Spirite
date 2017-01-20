package spirite.panel_work;

import java.awt.Color;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import javax.swing.Timer;

import jpen.PButton;
import jpen.PButtonEvent;
import jpen.PKindEvent;
import jpen.PLevel;
import jpen.PLevelEvent;
import jpen.PScrollEvent;
import jpen.event.PenListener;
import spirite.MUtil;
import spirite.brains.MasterControl;
import spirite.brains.PaletteManager;
import spirite.brains.ToolsetManager;
import spirite.brains.ToolsetManager.PixelSettings;
import spirite.brains.ToolsetManager.Tool;
import spirite.image_data.DrawEngine;
import spirite.image_data.DrawEngine.Method;
import spirite.image_data.DrawEngine.StrokeEngine;
import spirite.image_data.DrawEngine.StrokeParams;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageData;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.RenderEngine;
import spirite.image_data.RenderEngine.RenderSettings;
import spirite.image_data.SelectionEngine;
import spirite.image_data.SelectionEngine.Selection;
import spirite.image_data.SelectionEngine.SelectionType;
import spirite.image_data.UndoEngine;
import spirite.image_data.UndoEngine.StrokeAction;

/***
 * The Penner translates Pen and Mouse input, particularly from the draw
 * panel and then translates them into actions to be performed by the 
 * DrawEngine.
 * 
 * Uses the JPen2 library which requires the JPen DLLs to be accessible.
 *
 * @author Rory Burks
 */
public class Penner 
	implements PenListener, KeyEventDispatcher, ActionListener
{
	// Contains "Image to Screen" and "Screen to Image" methods.
	//	Could possibly wrap them in an interface to avoid tempting Penner 
	//	with UI controls
	private final WorkPanel context;	
	private final Timer update_timer;
	private StrokeEngine strokeEngine = null;
	
	// It might be easier to just link Master instead of linking every
	//	single Manager in the kitchen, but I like the idea of encouraging
	//	thinking about why you need a component before actually linking it.
	private final ImageWorkspace workspace;
	private final SelectionEngine selectionEngine;
	private final UndoEngine undoEngine;
	private final DrawEngine drawEngine;
	private final ToolsetManager toolsetManager;
	private final PaletteManager paletteManager;
	private final RenderEngine renderEngine;	// used for color picking.
												// might not belong here, maybe in DrawEngine
	
	private Tool activeTool = null;
	
	private int x, y;
	
	private enum STATE { READY, DRAWING, FORMING_SELECTION, MOVING_SELECTION, MOVING_NODE,
		PICKING};
	private STATE state = STATE.READY;
	private int stateVar = 0;
	
	public Penner( DrawPanel draw_panel, MasterControl master) {
		this.context = draw_panel.context;
		this.workspace = draw_panel.workspace;
		this.selectionEngine = workspace.getSelectionEngine();
		this.undoEngine = workspace.getUndoEngine();
		this.drawEngine = workspace.getDrawEngine();
		this.toolsetManager = master.getToolsetManager();
		this.paletteManager = master.getPaletteManager();
		this.renderEngine = master.getRenderEngine();

		// Add Timer and KeyDispatcher
		//	Note: since these are utilities with a global focus that you're
		//	giving references of Penner to, you will need to clean them up
		//	so that Penner (and everything it touches) gets GC'd
		update_timer = new Timer(16, this);
		update_timer.setRepeats(true);
		update_timer.start();
		
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.addKeyEventDispatcher(this);
	}
	
	// Since Penner mostly deals in Image Coordinates, not Screen Coordinates,
	//	when the Image Space changes (such as on zoom-in), the coordinates need
	//	to be updated.
	public void refreshCoordinates() {
		rawUpdateX(rawX);
		rawUpdateY(rawY);
	}

	// :::: PenListener
	@Override	public void penScrollEvent(PScrollEvent arg0) {}
	@Override	public void penTock(long arg0) {}
	@Override
	public void penButtonEvent(PButtonEvent pbe) {
		
		if( pbe.button.value == true) {
			x = wX;
			y = wY;
			shiftStartX = wX;
			shiftStartY = wY;
			shiftMode = 0;
			
			PButton.Type button = pbe.button.getType();
			
			if( button != PButton.Type.LEFT && button != PButton.Type.RIGHT && button != PButton.Type.CENTER)
				return;
			
			Tool tool = toolsetManager.getSelectedTool();
			
			switch( tool) {
			case PEN:
				if( holdingCtrl)  {
					pickColor(button == PButton.Type.LEFT);
					state = STATE.PICKING;
					stateVar = (button == PButton.Type.LEFT)?1:0;
				}
				else
					startPen( button == PButton.Type.LEFT);
				break;
			case ERASER:
				startErase();
				break;
			case FILL:
				fill( button == PButton.Type.LEFT);
				break;
			case BOX_SELECTION:
				startSelection();
				break;
			case MOVE:
				startMove();
				break;
			case COLOR_PICKER:
				pickColor( button == PButton.Type.LEFT);
				state = STATE.PICKING;
				stateVar = (button == PButton.Type.LEFT)?1:0;
				break;
			}
			
			activeTool = tool;
			
			if( selectionEngine.getSelection() != null
					&& state != STATE.FORMING_SELECTION
					&& state != STATE.MOVING_SELECTION) 
			{
				selectionEngine.unselect();
			}
			
		}
		else {
			// Pen-up
			switch( state) {
			case DRAWING:
				if( strokeEngine != null) {
					strokeEngine.endStroke();
					
					StrokeAction stroke = undoEngine.new StrokeAction( 
							strokeEngine.getParams(), 
							strokeEngine.getHistory());
					undoEngine.storeAction( stroke, strokeEngine.getImageData());
					strokeEngine = null;
				}
				break;
			case FORMING_SELECTION:
				selectionEngine.finishBuildingSelection();
				break;
			default:
				break;
			}
			state = STATE.READY;
		}
		
	}
	
	// :::: Start Methods
	private void startPen( boolean leftClick) {
		PixelSettings settings = (PixelSettings) toolsetManager.getToolsetSettings(Tool.PEN);
		StrokeParams stroke = new StrokeParams();
		Color c = (leftClick) ? 
				paletteManager.getActiveColor(0)
				: paletteManager.getActiveColor(1);
		stroke.setColor( c);
		stroke.setWidth(settings.getWidth());
		
		// Start the Stroke
		startStroke( stroke);
	}
	private void startErase() {
		StrokeParams stroke = new StrokeParams();
		stroke.setMethod( Method.ERASE);

		// Start the Stroke
		startStroke( stroke);
	}	
	private void startStroke( StrokeParams stroke) {
		if( workspace != null && workspace.getActiveData() != null) {
			ImageData data = workspace.getActiveData();
			GroupTree.Node node = workspace.getSelectedNode();

			strokeEngine = drawEngine.createStrokeEngine( data);
			
			if( strokeEngine.startStroke( stroke, x - node.getOffsetX() , y - node.getOffsetY())) {
				data.refresh();
			}
			state = STATE.DRAWING;
		}
	}
	private void startSelection() {
		Selection selection = selectionEngine.getSelection();
		
		if( selection != null && selection.contains(x,y)) {
			if( !selectionEngine.isLifted())
				selectionEngine.liftSelection();
			state = STATE.MOVING_SELECTION;
		}
		else {
			selectionEngine.startBuildingSelection(SelectionType.RECTANGLE, x, y);
			state = STATE.FORMING_SELECTION;
		}
	}
	private void startMove() {
		Selection selection = selectionEngine.getSelection();
		
		if(selection != null) {
			if( !selectionEngine.isLifted())
				selectionEngine.liftSelection();
			
			state = STATE.MOVING_SELECTION;
		}
		else {
			state = STATE.MOVING_NODE;
		}
	}
	private void pickColor( boolean leftClick) {
		// Get the composed image
		RenderSettings settings = new RenderSettings();
		settings.workspace = workspace;
		BufferedImage img = renderEngine.renderImage(settings);
		
		if( !MUtil.coordInImage(x, y, img))
			return;
		paletteManager.setActiveColor(
				(leftClick)?0:1, new Color(img.getRGB(x, y)));
	}
	private void fill( boolean leftClick) {
		// Determine Color
		Color c = (leftClick) ? 
				paletteManager.getActiveColor(0)
				: paletteManager.getActiveColor(1);

		// Grab the Active Data
		ImageData data = workspace.getActiveData();
		GroupTree.Node node = workspace.getSelectedNode();
		
		if( data != null && node != null) {
			// Perform the fill Action, only store the UndoAction if 
			//	an actual change is made.
			Point p = new Point(x - node.getOffsetX(), y - node.getOffsetY());
			if( drawEngine.fill( p.x, p.y, c, data)) {
				undoEngine.storeAction( undoEngine.new FillAction(p, c) , data);
			}
		} 
	}
	
	

	@Override
	public void penKindEvent(PKindEvent pke) {
		switch( pke.kind.getType()) {
		case CURSOR:
			toolsetManager.setCursor(ToolsetManager.Cursor.MOUSE);
			break;
		case STYLUS:
			toolsetManager.setCursor(ToolsetManager.Cursor.STYLUS);
			break;
		case ERASER:
			toolsetManager.setCursor(ToolsetManager.Cursor.ERASER);
			break;
		default:
			break;
		}
		
	}

	private boolean holdingShift = false;
	private boolean holdingCtrl = false;
	private int shiftMode = 0;	// 0 : accept any, 1 : horizontal, 2: vertical
	private int shiftStartX;
	private int shiftStartY;
	private int wX;		// wX and wY are the semi-raw coordinates which are just
	private int wY;		// 	the raw positions converted to ImageSpace whereas x and y
						// 	sometimes do not get updated (e.g. when shift-locked)
	private int oldX;	// OldX and OldY are the last-checked X and Y primarily used
	private int oldY;	// 	for things that only happen if they change
	private int rawX;	// raw position are the last-recorded coordinates in pure form
	private int rawY;	// 	(screen coordinates relative to the component Penner watches over)
	
	private void rawUpdateX( int raw) {
		rawX = raw;
		wX = context.stiXm(rawX);
		if( holdingShift && state == STATE.DRAWING) {
			if( shiftMode == 2)
				return;
			if( shiftMode == 0) {
				if( Math.abs(shiftStartX - wX) > 10) {
					shiftMode = 1;
				}
				else return;
			}
		}
		x = wX;
	}
	private void rawUpdateY( int raw) {
		rawY = raw;
		wY = context.stiYm( rawY);
		if( holdingShift && state == STATE.DRAWING) {
			if( shiftMode == 1)
				return;
			if( shiftMode == 0) {
				if( Math.abs(shiftStartY - wY) > 10) {
					shiftMode = 2;
				}
				else return;
			}
		}
		y = wY;
	}
	@Override
	public void penLevelEvent(PLevelEvent ple) {
		// Note: JPen updates PenLevels (which inform of things like position and pressure)
		//	asynchronously with press buttons and other such things, so you have to be careful.
		for( PLevel level: ple.levels) {
			switch( level.getType()) {
			case X:
				rawUpdateX(Math.round(level.value));
				break;
			case Y:
				rawUpdateY(Math.round(level.value));
				break;
			case PRESSURE:
			default:
				break;
			}
		}
		
		context.refreshCoordinates(x, y);
		GroupTree.Node node= workspace.getSelectedNode();// !!!! Maybe better to store which node you're moving locally
		
		// Perform state-based "on-pen/mouse move" code
		switch( state) {
		case DRAWING:
			if( strokeEngine != null && node != null) {
				strokeEngine.updateStroke(x - node.getOffsetX(), y - node.getOffsetY());
			}
			break;
		case FORMING_SELECTION:
			selectionEngine.updateBuildingSelection(x, y);
			break;
		case MOVING_SELECTION:
			if( oldX != x || oldY != y) 
				selectionEngine.setOffset(
						selectionEngine.getOffsetX() + (x - oldX),
						selectionEngine.getOffsetY() + (y - oldY));
			break;
		case MOVING_NODE:
			if( node != null && (oldX != x || oldY != y))
				node.setOffsetX( node.getOffsetX() + (x - oldX), 
								 node.getOffsetY() + (y - oldY));
			break;
		case PICKING:
			pickColor( stateVar == 1);
			break;
		default:
			break;
		}
		
		oldX = x;
		oldY = y;
	}

	

	// :::: KeyEventDispatcher
	@Override
	public boolean dispatchKeyEvent(KeyEvent evt) {
		boolean shift =(( evt.getModifiers() & KeyEvent.SHIFT_MASK) != 0);
		boolean ctrl = (( evt.getModifiers() & KeyEvent.CTRL_MASK) != 0);
		if( shift && !holdingShift) {
			shiftStartX = x;
			shiftStartY = y;
			shiftMode = 0;
		}
			
		holdingShift = shift;
		holdingCtrl = ctrl;
		return false;
	}
	
	// :::: ActionListener
	@Override
	public void actionPerformed(ActionEvent evt) {
		if( strokeEngine != null && state == STATE.DRAWING) {
			if( strokeEngine.stepStroke()) {
				strokeEngine.getImageData().refresh();
			}
		}
	}
	

	/** Cleans up resources that have a global-level context in Swing to avoid
	 * Memory Leaks. */
	public void cleanUp() {
		update_timer.stop();
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.removeKeyEventDispatcher(this);
		
	}

}
