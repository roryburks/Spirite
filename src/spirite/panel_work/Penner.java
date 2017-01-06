package spirite.panel_work;

import java.awt.Color;
import java.util.Timer;
import java.util.TimerTask;

import jpen.PButton;
import jpen.PButtonEvent;
import jpen.PKind;
import jpen.PKindEvent;
import jpen.PLevel;
import jpen.PLevelEvent;
import jpen.PScrollEvent;
import jpen.event.PenListener;
import spirite.brains.MasterControl;
import spirite.brains.ToolsetManager;
import spirite.draw_engine.DrawEngine.StrokeParams;
import spirite.draw_engine.DrawEngine.StrokeParams.Method;

public class Penner 
	implements PenListener
{
	WorkPanel context;
	DrawPanel draw_panel;
	MasterControl master;
	
	int x, y;
	
	private enum STATE { READY, DRAWING};
	STATE state = STATE.READY;
	
	public Penner( DrawPanel draw_panel) {
		this.draw_panel = draw_panel;
		this.context = draw_panel.context;
		this.master = context.master;
		
	}

	@Override
	public void penButtonEvent(PButtonEvent pbe) {
		PKind.Type type = pbe.pen.getKind().getType();
		
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
				
				master.getDrawEngine().startStroke(stroke, context.stiXm(x), context.stiYm(y));
				state = STATE.DRAWING;
			}
			if( tool == "eraser") {
				StrokeParams stroke = new StrokeParams();
				stroke.setMethod( Method.ERASE);

				master.getDrawEngine().startStroke(stroke, context.stiXm(x), context.stiYm(y));
				state = STATE.DRAWING;
			}
			if( tool == "fill") {
				Color c = (button == PButton.Type.LEFT) ? 
						context.master.getPaletteManager().getActiveColor(0)
						: context.master.getPaletteManager().getActiveColor(1);
				master.getDrawEngine().fill(context.stiXm(x), context.stiYm(y), c);
			}
			
		}
		else if( state == STATE.DRAWING) {
			master.getDrawEngine().endStroke();
			state = STATE.READY;
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
			}
		}
		
		if( state == STATE.DRAWING) {
			master.getDrawEngine().updateStroke( context.stiXm(x), context.stiYm(y));
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
