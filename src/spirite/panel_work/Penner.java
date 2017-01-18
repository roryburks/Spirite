package spirite.panel_work;

import java.awt.Color;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.Timer;

import jpen.PButton;
import jpen.PButtonEvent;
import jpen.PKindEvent;
import jpen.PLevel;
import jpen.PLevelEvent;
import jpen.PScrollEvent;
import jpen.event.PenListener;
import spirite.brains.MasterControl;
import spirite.brains.PaletteManager;
import spirite.brains.ToolsetManager;
import spirite.image_data.DrawEngine;
import spirite.image_data.DrawEngine.Method;
import spirite.image_data.DrawEngine.StrokeEngine;
import spirite.image_data.DrawEngine.StrokeParams;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageData;
import spirite.image_data.ImageWorkspace;
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
	//	single Manager in the kitchen, but I don't like the idea of 
	//	leaking capabilities (such as frame management) to components that 
	//	don't need it.
	private final ImageWorkspace workspace;
	private final SelectionEngine selectionEngine;
	private final UndoEngine undoEngine;
	private final DrawEngine drawEngine;
	private final ToolsetManager toolsetManager;
	private final PaletteManager paletteManager;
	
	private String activeTool = null;
	
	private int x, y;
	
	private enum STATE { READY, DRAWING, FORMING_SELECTION, MOVING_SELECTION, MOVING_NODE};
	private STATE state = STATE.READY;
	
	public Penner( DrawPanel draw_panel, MasterControl master) {
		this.context = draw_panel.context;
		this.workspace = draw_panel.workspace;
		this.selectionEngine = workspace.getSelectionEngine();
		this.undoEngine = workspace.getUndoEngine();
		this.drawEngine = workspace.getDrawEngine();
		this.toolsetManager = master.getToolsetManager();
		this.paletteManager = master.getPaletteManager();

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
			
			String tool = toolsetManager.getSelectedTool();
			
			if( tool.equals("pen")) {
				StrokeParams stroke = new StrokeParams();
				Color c = (button == PButton.Type.LEFT) ? 
						paletteManager.getActiveColor(0)
						: paletteManager.getActiveColor(1);
				stroke.setColor( c);
				
				// Start the Stroke
				startStroke( stroke);
			}
			if( tool.equals("eraser")) {
				StrokeParams stroke = new StrokeParams();
				stroke.setMethod( Method.ERASE);

				// Start the Stroke
				startStroke( stroke);
			}
			if( tool.equals("fill")) {
				// Determine Color
				Color c = (button == PButton.Type.LEFT) ? 
						paletteManager.getActiveColor(0)
						: paletteManager.getActiveColor(1);

				// Grab the Active Data
				ImageData data = workspace.getActiveData();
				
				if( data != null) {
					// Perform the fill Action, only store the UndoAction if 
					//	an actual change is made.
					Point p = new Point(x, y);
					if( drawEngine.fill( p.x, p.y, c, data)) {
						undoEngine.storeAction( undoEngine.new FillAction(p, c) , data);
					}
				} 
			}
			if( tool.equals("box_selection")){
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
			if( tool.equals("move")) {
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
	
	private void startStroke( StrokeParams stroke) {
		if( workspace != null && workspace.getActiveData() != null) {
			ImageData data = workspace.getActiveData();

			strokeEngine = drawEngine.createStrokeEngine( data);
			
			if( strokeEngine.startStroke( stroke, x, y)) {
				data.refresh();
			}
			state = STATE.DRAWING;
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
	private int shiftMode = 0;	// 0 : accept any, 1 : horizontal, 2: vertical
	private int shiftStartX;
	private int shiftStartY;
	private int wX;
	private int wY;
	private int oldX;
	private int oldY;
	@Override
	public void penLevelEvent(PLevelEvent ple) {
		// Note: JPen updates PenLevels (which inform of things like position and pressure)
		//	asynchronously with press buttons and other such things, so you have to be careful.
		for( PLevel level: ple.levels) {
			switch( level.getType()) {
			case X:
				wX = context.stiXm(Math.round( level.value));
				if( holdingShift && state == STATE.DRAWING) {
					if( shiftMode == 2)
						break;
					if( shiftMode == 0) {
						if( Math.abs(shiftStartX - wX) > 10) {
							shiftMode = 1;
						}
						else break;
					}
				}
				
				x = wX;
				break;
			case Y:
				wY = context.stiYm(Math.round( level.value));
				if( holdingShift && state == STATE.DRAWING) {
					if( shiftMode == 1)
						break;
					if( shiftMode == 0) {
						if( Math.abs(shiftStartY - wY) > 10) {
							shiftMode = 2;
						}
						else break;
					}
				}
				y = wY;
				break;
			case PRESSURE:
			default:
				break;
			}
		}
		
		
		
		context.refreshCoordinates(x, y);
		
		// Perform state-based "on-pen/mouse move" code
		switch( state) {
		case DRAWING:
			if( strokeEngine != null) {
				strokeEngine.updateStroke(x, y);
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
			GroupTree.Node node = workspace.getSelectedNode();	// !!!! Maybe better to store which node you're moving locally
			if( node != null && (oldX != x || oldY != y))
				node.setOffsetX( node.getOffsetX() + (x - oldX), 
								 node.getOffsetY() + (y - oldY));
				
		default:
			break;
		}
		
		oldX = x;
		oldY = y;
	}
	@Override	public void penScrollEvent(PScrollEvent arg0) {}
	@Override	public void penTock(long arg0) {}

	

	// :::: KeyEventDispatcher
	@Override
	public boolean dispatchKeyEvent(KeyEvent evt) {
		boolean shift =(( evt.getModifiers() & KeyEvent.SHIFT_MASK) != 0);
		if( shift && !holdingShift) {
			shiftStartX = x;
			shiftStartY = y;
			shiftMode = 0;
		}
			
		holdingShift = shift;
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
