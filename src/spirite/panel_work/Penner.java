package spirite.panel_work;

import java.awt.Color;
import java.awt.Point;
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
import spirite.image_data.ImageData;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.UndoEngine;
import spirite.image_data.DrawEngine.Method;
import spirite.image_data.DrawEngine.StrokeEngine;
import spirite.image_data.DrawEngine.StrokeParams;
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
	implements PenListener
{
	// Why it needs Master: it needs access to know which Toolset and Palette Colors
	//	are being used.  !! DO NOT USE master.getCurrentWorkspace !!
	MasterControl master;
	WorkPanel context;
	DrawPanel drawPanel;
	Timer update_timer;
	StrokeEngine strokeEngine = null;
	
	int x, y;
	
	private enum STATE { READY, DRAWING};
	STATE state = STATE.READY;
	
	public Penner( DrawPanel draw_panel) {
		this.drawPanel = draw_panel;
		this.context = draw_panel.context;
		this.master = context.master;

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
	}

	@Override
	public void penButtonEvent(PButtonEvent pbe) {
//		PKind.Type type = pbe.pen.getKind().getType();
		
		if( pbe.button.value == true) {
			PButton.Type button = pbe.button.getType();
			
			if( button != PButton.Type.LEFT && button != PButton.Type.RIGHT && button != PButton.Type.CENTER)
				return;
			
			String tool = master.getToolsetManager().getSelectedTool();
			
			if( tool == "pen") {
				StrokeParams stroke = new StrokeParams();
				Color c = (button == PButton.Type.LEFT) ? 
						context.master.getPaletteManager().getActiveColor(0)
						: context.master.getPaletteManager().getActiveColor(1);
				stroke.setColor( c);
				
				// Start the Stroke
				startStroke( stroke);
			}
			if( tool == "eraser") {
				StrokeParams stroke = new StrokeParams();
				stroke.setMethod( Method.ERASE);

				// Start the Stroke
				startStroke( stroke);
			}
			if( tool == "fill") {
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
					Point p = new Point(context.stiXm(x), context.stiYm(y));
					UndoEngine engine = workspace.getUndoEngine();
					engine.prepareContext(data);
					if( master.getDrawEngine().fill( p.x, p.y, c, data)) {
						engine.storeAction( engine.new FillAction(p, c) , data);
						data.refresh();
					}
				} 
			}
			
		}
		else if( state == STATE.DRAWING) {
			// Pen-up
			
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
			state = STATE.READY;
		}
		
	}
	
	private void startStroke( StrokeParams stroke) {
		ImageWorkspace workspace = drawPanel.workspace;
		if( workspace != null && workspace.getActiveData() != null) {
			ImageData data = workspace.getActiveData();
			workspace.getUndoEngine().prepareContext(data);
			strokeEngine = master.getDrawEngine().createStrokeEngine( data);
			
			if( strokeEngine.startStroke( stroke, context.stiXm(x), context.stiYm(y))) {
				workspace.triggerImageRefresh();
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

	@Override
	public void penLevelEvent(PLevelEvent ple) {
		// Note: JPen updates PenLevels (which inform of things like position and pressure)
		//	asynchronously with press buttons and other such things, so you have to be careful.
		for( PLevel level: ple.levels) {
			switch( level.getType()) {
			case X:
				x = Math.round( level.value);
				break;
			case Y:
				y = Math.round( level.value);
				break;
			case PRESSURE:
			default:
				break;
			}
		}
		
		if( state == STATE.DRAWING) {
			if( strokeEngine != null) {
				strokeEngine.updateStroke(context.stiXm(x), context.stiYm(y));
			}
		}
	}

	@Override	public void penScrollEvent(PScrollEvent arg0) {}
	@Override	public void penTock(long arg0) {}

}
