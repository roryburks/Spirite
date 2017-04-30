package spirite.hybrid;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

public class HybridTimer implements ActionListener {
	private final Timer timer;
	private final Runnable action;
	
	public HybridTimer( int milliseconds, Runnable action) {
		timer = new Timer( milliseconds, this);
		this.action = action;
	}

	public void start() {
		timer.start();
	}
	
	public void stop() {
		timer.stop();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		action.run();
	}
	
}
