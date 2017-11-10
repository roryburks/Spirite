package spirite.pc.pen;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

import jpen.PButtonEvent;
import jpen.PKindEvent;
import jpen.PLevel;
import jpen.PLevelEvent;
import jpen.PScrollEvent;
import jpen.event.PenListener;
import spirite.base.brains.MasterControl;
import spirite.base.brains.ToolsetManager;
import spirite.base.graphics.GraphicsContext;
import spirite.base.pen.PenTraits.ButtonType;
import spirite.base.pen.PenTraits.MButtonEvent;
import spirite.base.pen.Penner;
import spirite.hybrid.HybridHelper;
import spirite.pc.ui.panel_work.WorkPanel;
/**
 * 
 * Uses the JPen2 library.
 * 
 * NOTE: Since JPen is running asynchronously, queueToRun should be used.
 *
 */
public class JPenPenner implements PenListener
{
	private final ToolsetManager toolsetManager;
	public final Penner penner;
	
	public JPenPenner( WorkPanel context, MasterControl master) {
		penner = new Penner(context, master);
		toolsetManager = master.getToolsetManager();

		KeyboardFocusManager.getCurrentKeyboardFocusManager()
		.addKeyEventDispatcher( dispatcher);
	}
	KeyEventDispatcher dispatcher = new KeyEventDispatcher() {
		@Override
		public boolean dispatchKeyEvent(KeyEvent evt) {
			boolean shift =(( evt.getModifiers() & KeyEvent.SHIFT_MASK) != 0);
			boolean ctrl = (( evt.getModifiers() & KeyEvent.CTRL_MASK) != 0);
			boolean alt = (( evt.getModifiers() & KeyEvent.ALT_MASK) != 0);
				
			penner.holdingShift = shift;
			penner.holdingCtrl = ctrl;
			penner.holdingAlt = alt;
			return false;
		}
	};
	
	

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

	@Override public void penScrollEvent(PScrollEvent arg0) {}
	@Override
	public void penButtonEvent(PButtonEvent evt) {
		if( evt.button.value) {
			HybridHelper.queueToRun(() -> {
				penner.penDownEvent(MButtonFromPButton(evt));
			});
		}
		else {
			HybridHelper.queueToRun(() -> {
				penner.penUpEvent(MButtonFromPButton(evt));
			});
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
		HybridHelper.queueToRun(() -> {penner.step();});
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

	public void paintOverlay(GraphicsContext gc) {
		penner.paintOverlay(gc);
	}

	/** Cleans up resources that have a global-level context in Swing to avoid
	 * Memory Leaks. 
	 * 
	 * NOTE: Never called
	 * */
	final
	public void cleanUp() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher);
	}
}
