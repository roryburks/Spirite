package spirite.panel_work;

import java.awt.Color;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.util.Timer;
import java.util.TimerTask;

import jpen.PButton;
import jpen.PButtonEvent;
import jpen.PKindEvent;
import jpen.PLevel;
import jpen.PLevelEvent;
import jpen.PScrollEvent;
import jpen.event.PenListener;
import spirite.brains.MasterControl;
import spirite.brains.ToolsetManager;
import spirite.image_data.DrawEngine.Method;
import spirite.image_data.DrawEngine.StrokeEngine;
import spirite.image_data.DrawEngine.StrokeParams;
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
	implements PenListener, KeyEventDispatcher
{
	// Why it needs Master: it needs access to know which Toolset and Palette Colors
	//	are being used.  !! DO NOT USE master.getCurrentWorkspace !!
	MasterControl master;
	WorkPanel context;
	DrawPanel drawPanel;
	Timer update_timer;
	StrokeEngine strokeEngine = null;
	
	ImageWorkspace workspace;
	SelectionEngine selectionEngine;
	UndoEngine undoEngine;
	
	String activeTool = null;
	
	int x, y;
	
	private enum STATE { READY, DRAWING, FORMING_SELECTION, MOVING};
	STATE state = STATE.READY;
	
	public Penner( DrawPanel draw_panel) {
		this.drawPanel = draw_panel;
		this.context = draw_panel.context;
		this.master = context.master;
		this.workspace = draw_panel.workspace;
		this.selectionEngine = workspace.getSelectionEngine();
		this.undoEngine = workspace.getUndoEngine();

		// Add Timer and KeyDispatcher
		//	Note: since these are utilities with a global focus that you're
		//	giving references of Penner to, you will need to clean them up
		//	so that Penner (and everything it touches) gets GC'd
		update_timer = new Timer();
		update_timer.scheduleAtFixedRate( new TimerTask() {
			@Override
			public void run() {
				if( strokeEngine != null && state == STATE.DRAWING) {
					if( strokeEngine.stepStroke()) {
						strokeEngine.getImageData().refresh();
					}
				}
			}
			
		}, 100, 16);
		
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
			
			String tool = master.getToolsetManager().getSelectedTool();
			
			if( tool.equals("pen")) {
				StrokeParams stroke = new StrokeParams();
				Color c = (button == PButton.Type.LEFT) ? 
						context.master.getPaletteManager().getActiveColor(0)
						: context.master.getPaletteManager().getActiveColor(1);
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
						master.getPaletteManager().getActiveColor(0)
						: master.getPaletteManager().getActiveColor(1);

				// Grab the Active Data
				ImageWorkspace workspace = drawPanel.workspace;
				ImageData data = workspace.getActiveData();
				
				if( data != null) {
					// Perform the fill Action, only store the UndoAction if 
					//	an actual change is made.
					Point p = new Point(x, y);
					UndoEngine engine = workspace.getUndoEngine();
					engine.prepareContext(data);
					if( drawPanel.workspace.getDrawEngine().fill( p.x, p.y, c, data)) {
						engine.storeAction( engine.new FillAction(p, c) , data);
					}
				} 
			}
			if( tool.equals("box_selection")){
				Selection selection = selectionEngine.getSelection();
				
				if( selection != null && selection.contains(x,y)) {
					state = STATE.MOVING;
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
					
					state = STATE.MOVING;
				}
			}
			
			activeTool = tool;
			
		}
		else {
			// Pen-up
			switch( state) {
			case DRAWING:
				if( strokeEngine != null) {
					strokeEngine.endStroke();
					
					// TODO : This should probably not be polling master, but instead StrokeEngine somehow
					UndoEngine engine = drawPanel.workspace.getUndoEngine();
					StrokeAction stroke = engine.new StrokeAction( 
							strokeEngine.getParams(), 
							strokeEngine.getHistory());
					engine.storeAction( stroke, strokeEngine.getImageData());
					strokeEngine = null;
				}
				break;
			case FORMING_SELECTION:
				drawPanel.workspace.getSelectionEngine().finishBuildingSelection();
//				drawPanel.workspace.getSelectionEngine().liftSelection();
			}
			state = STATE.READY;
		}
		
	}
	
	private void startStroke( StrokeParams stroke) {
		ImageWorkspace workspace = drawPanel.workspace;
		if( workspace != null && workspace.getActiveData() != null) {
			ImageData data = workspace.getActiveData();
			workspace.getUndoEngine().prepareContext(data);

			strokeEngine = drawPanel.workspace.getDrawEngine().createStrokeEngine( data);
			
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
			master.getToolsetManager().setCursor(ToolsetManager.Cursor.MOUSE);
			break;
		case STYLUS:
			master.getToolsetManager().setCursor(ToolsetManager.Cursor.STYLUS);
			break;
		case ERASER:
			master.getToolsetManager().setCursor(ToolsetManager.Cursor.ERASER);
			break;
		default:
			break;
		}
		
	}

	boolean holdingShift = false;
	int shiftMode = 0;	// 0 : accept any, 1 : horizontal, 2: vertical
	int shiftStartX;
	int shiftStartY;
	int wX;
	int wY;
	int oldX;
	int oldY;
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
		
		
		
		context.workSplicePanel.context.refreshCoordinates(x, y);
		
		// Perform state-based "on-pen/mouse move" code
		switch( state) {
		case DRAWING:
			if( strokeEngine != null) {
				strokeEngine.updateStroke(x, y);
			}
			break;
		case FORMING_SELECTION:
			drawPanel.workspace.getSelectionEngine().updateBuildingSelection(x, y);
			break;
		case MOVING:
			if( oldX != x || oldY != y) {
				selectionEngine.setOffset(
						selectionEngine.getOffsetX() + (x - oldX),
						selectionEngine.getOffsetY() + (y - oldY));
			}
			break;
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
	

	public void cleanUp() {
		update_timer.cancel();
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.removeKeyEventDispatcher(this);
		
	}
}
