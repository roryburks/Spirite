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
import spirite.draw_engine.DrawEngine.StrokeEngine;
import spirite.draw_engine.DrawEngine.StrokeParams;
import spirite.draw_engine.DrawEngine.StrokeParams.Method;
import spirite.draw_engine.UndoEngine;
import spirite.draw_engine.UndoEngine.StrokeAction;
import spirite.image_data.ImageData;
import spirite.image_data.ImageWorkspace;

public class Penner 
	implements PenListener
{
	WorkPanel context;
	DrawPanel draw_panel;
	MasterControl master;
	Timer update_timer;
	StrokeEngine strokeEngine = null;
	
	int x, y;
	
	private enum STATE { READY, DRAWING};
	STATE state = STATE.READY;
	
	public Penner( DrawPanel draw_panel) {
		this.draw_panel = draw_panel;
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
				Color c = (button == PButton.Type.LEFT) ? 
						master.getPaletteManager().getActiveColor(0)
						: master.getPaletteManager().getActiveColor(1);

				ImageWorkspace workspace = master.getCurrentWorkspace();
				ImageData data = workspace.getActiveData();
				if( data != null) {
					Point p = new Point(context.stiXm(x), context.stiYm(y));
					UndoEngine engine = workspace.getUndoEngine();
					engine.prepareContext(data);
					master.getDrawEngine().fill( p.x, p.y, c, data);
					engine.storeAction( engine.new FillAction(p, c) , data);
					data.refresh();
				} 
			}
			
		}
		else if( state == STATE.DRAWING) {
			// End the Stroke
			if( strokeEngine != null) {
				strokeEngine.endStroke();
				
				// TODO : This should probably not be polling master, but instead StrokeEngine somehow
				UndoEngine engine = master.getCurrentWorkspace().getUndoEngine();
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
		ImageWorkspace workspace = master.getCurrentWorkspace();
		if( workspace != null && workspace.getActiveData() != null) {
			ImageData data = workspace.getActiveData();
			workspace.getUndoEngine().prepareContext(data);
			strokeEngine = master.getDrawEngine().createStrokeEngine( data);
			strokeEngine.startStroke( stroke, context.stiXm(x), context.stiYm(y));
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
		
		// TODO Auto-generated method stub
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

	@Override
	public void penScrollEvent(PScrollEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void penTock(long arg0) {
		
	}

}
