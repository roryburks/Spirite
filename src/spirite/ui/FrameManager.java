package spirite.ui;

import java.awt.Container;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import spirite.RootFrame;
import spirite.brains.MasterControl;

public class FrameManager implements WindowListener {
	MasterControl master;
	RootFrame root = null;;
	
	List<JDialog> frames = new ArrayList<>();
	
	public FrameManager( MasterControl master) {
		this.master = master;
	}
	
	public void packMainFrame() {
        root = new RootFrame( master);
        root.pack();
        root.setLocationByPlatform(true);
        root.setVisible(true);
	}
	
	public void addFrame( JPanel panel) {
		JDialog container = new JDialog();
		
		container.add( panel);
		container.pack();
		
		if( root != null) {
			Point p = root.getLocationOnScreen();
			container.setLocation( p.x + root.getWidth(), p.y);
		}
		
		container.setVisible(true);
		container.addWindowListener(this);
		frames.add(container);
	}
	
	public void showAllFrames() {
		for( JDialog frame : frames) {
			frame.toFront();
		}
	}

	
	// :::: WindowListener
	@Override
	public void windowClosed(WindowEvent evt) {
		System.out.println(frames.indexOf(evt.getWindow()));
	}
	@Override	public void windowActivated(WindowEvent arg0) {}
	@Override	public void windowClosing(WindowEvent arg0) {}
	@Override	public void windowDeactivated(WindowEvent arg0) {}
	@Override	public void windowDeiconified(WindowEvent arg0) {}
	@Override	public void windowIconified(WindowEvent arg0) {}
	@Override	public void windowOpened(WindowEvent arg0) {}
}
