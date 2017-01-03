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

import spirite.brains.MasterControl;
import spirite.panel_layers.LayersPanel;
import spirite.panel_toolset.ToolsPanel;
import spirite.ui.OmniFrame.OmniPanel;

public class FrameManager implements WindowListener {
	private MasterControl master;
	private RootFrame root = null;
	
	private List<OmniFrame> frames = new ArrayList<>();
	
	public static enum FrameType {
		BAD (""),
		LAYER ("Layers"),
		TOOLS ("Toolset"),
		;
		
		private String name;
		FrameType( String str) {
			name = str;
		}
		public String getName() {
			return name;
		}
	}
	
	public OmniPanel createOmniPanel( FrameType type) {
		switch( type) {
		case LAYER:
			return new LayersPanel( master);
		case TOOLS:
			return new ToolsPanel(master);
		default:
			return null;
		}
	}
	
	public FrameManager( MasterControl master) {
		this.master = master;
	}
	
	// :::: API
	public void performCommand( String command) {
		if( command.equals("showLayerFrame"))
			addFrame( FrameType.LAYER);
		else if( command.equals("showToolsFrame"))
			addFrame( FrameType.TOOLS);
	}
	
	// :::: UI-related
	public void packMainFrame() {
        root = new RootFrame( master);
        root.pack();
        root.setLocationByPlatform(true);
        root.setVisible(true);
	}
	
	/***
	 * 
	 * @param bit_mask Bitwise combination of 
	 */
	public void addFrame( FrameType frameType) {
		// First Check to make sure the frame type isn't already open
		//	(assuming it's not duplicateable)
		for( OmniFrame frame : frames) {
			if( frame.containsFrameType( frameType)) {
				frame.toFront();
				
				return;
			}
		}
		
		// Next create the container frame and show it
		OmniFrame container = new OmniFrame( master, frameType);
		
		container.addPanel( FrameType.TOOLS);
		container.pack();
		
		if( root != null) {
			Point p = root.getLocationOnScreen();
			container.setLocation( p.x + root.getWidth(), p.y);
		}
		
		container.setVisible(true);
		container.addWindowListener(this);
		frames.add(container);
	}
	
	/***
	 * 
	 */
	public void showAllFrames() {
		for( OmniFrame frame : frames) {
			frame.toFront();
		}
	}
	
	// :::: WindowListener
	@Override	public void windowClosing(WindowEvent evt) {
		frames.remove(evt.getWindow());
	}
	@Override	public void windowActivated(WindowEvent evt) {}
	@Override	public void windowClosed(WindowEvent evt) {}
	@Override	public void windowDeactivated(WindowEvent evt) {}
	@Override	public void windowDeiconified(WindowEvent evt) {}
	@Override	public void windowIconified(WindowEvent evt) {}
	@Override	public void windowOpened(WindowEvent evt) {}
}
