package spirite.ui;

import java.awt.Point;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import spirite.RootFrame;
import spirite.brains.MasterControl;

public class FrameManager {
	MasterControl master;
	RootFrame root = null;;
	
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
	}
}
