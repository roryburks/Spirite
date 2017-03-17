package spirite.panel_work;

import java.awt.Graphics;

import javax.swing.SwingUtilities;

import jpen.PButtonEvent;
import jpen.PKindEvent;
import jpen.PLevel;
import jpen.PLevelEvent;
import jpen.PScrollEvent;
import jpen.event.PenListener;
import spirite.brains.MasterControl;
import spirite.brains.ToolsetManager;

public class JPenPenner implements PenListener
{
	private final ToolsetManager toolsetManager;
	private final Penner penner;
	
	public JPenPenner( DrawPanel context, MasterControl master) {
		penner = new Penner(context, master);
		toolsetManager = master.getToolsetManager();
	}

	@Override	public void penScrollEvent(PScrollEvent arg0) {}
	@Override
	public void penButtonEvent(PButtonEvent evt) {
		if( evt.button.value) {
			SwingUtilities.invokeLater( new Runnable() {@Override public void run() {
				penner.penDownEvent(evt);
			}});
		}
		else {
			SwingUtilities.invokeLater( new Runnable() {@Override public void run() {
				penner.penUpEvent(evt);
			}});
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

	@Override
	public void penLevelEvent(PLevelEvent ple) {
		// Note: JPen updates PenLevels (which inform of things like position and pressure)
		//	asynchronously with press buttons and other such things, so you have to be careful.
		for( PLevel level: ple.levels) {
			switch( level.getType()) {
			case X:
				penner.rawUpdateX(Math.round(level.value));
				break;
			case Y:
				penner.rawUpdateY(Math.round(level.value));
				break;
			case PRESSURE:
				System.out.println(level.value);
				penner.rawUpdatePressure(level.value);
				break;
			default:
				break;
			}
		}
		
	}


	@Override
	public void penTock(long arg0) {
		SwingUtilities.invokeLater( new Runnable() {@Override public void run() {
			penner.step();
		}});
	}

	
	// These methods existing suggests that either JPenPenner should be a sub-class
	//	of Penner (which is stylistically appropriate but oddly composed) or should
	//	be able to pass its inner component which should be more stylistically 
	//	distinct
	public void refreshCoordinates() {
		penner.refreshCoordinates();
	}

	public boolean drawsOverlay() {
		return penner.drawsOverlay();
	}

	public void paintOverlay(Graphics g) {
		penner.paintOverlay(g);
	}

	public void cleanUp() {
		penner.cleanUp();
	}

	
	
}
