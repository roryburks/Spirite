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
import spirite.pen.PenTraits.ButtonType;
import spirite.pen.PenTraits.MButtonEvent;

public class JPenPenner implements PenListener
{
	private final ToolsetManager toolsetManager;
	private final Penner penner;
	
	public JPenPenner( DrawPanel context, MasterControl master) {
		penner = new Penner(context, master);
		toolsetManager = master.getToolsetManager();
	}

	private final MButtonEvent MButtonFromPButton( PButtonEvent pbe) {
		MButtonEvent mbe = new MButtonEvent();
		
		switch (pbe.button.getType()) {
		case LEFT:
			mbe.buttonType = ButtonType.LEFT;
			break;
		case RIGHT:
			mbe.buttonType = ButtonType.RIGHT;
			break;
		case CENTER:
			mbe.buttonType = ButtonType.CENTER;
			break;
		default:
			return null;
		}
		
		return mbe;
	}

	@Override	public void penScrollEvent(PScrollEvent arg0) {}
	@Override
	public void penButtonEvent(PButtonEvent evt) {
		if( evt.button.value) {
			SwingUtilities.invokeLater( new Runnable() {@Override public void run() {
				penner.penDownEvent(MButtonFromPButton(evt));
			}});
		}
		else {
			SwingUtilities.invokeLater( new Runnable() {@Override public void run() {
				penner.penUpEvent(MButtonFromPButton(evt));
			}});
		}
	}

	@Override
	public void penKindEvent(PKindEvent pke) {
		switch( pke.kind.getType()) {
		case CURSOR:
			penner.rawUpdatePressure(1.0f);
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
