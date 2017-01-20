package spirite.ui;

import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import spirite.brains.MasterControl;
import spirite.brains.ToolsetManager.PixelSettings;
import spirite.brains.ToolsetManager.Tool;
import spirite.panel_anim.AnimPanel;
import spirite.panel_anim.AnimationSchemePanel;
import spirite.panel_layers.LayersPanel;
import spirite.panel_toolset.ToolsPanel;
import spirite.panel_toolset.UndoPanel;
import spirite.panel_toolset.settings_panels.PixelSettingsPanel;
import spirite.ui.OmniFrame.OmniContainer;

public class FrameManager implements WindowListener {
	private final MasterControl master;
	private RootFrame root = null;
	
	private final List<OmniFrame> frames = new ArrayList<>();

	public FrameManager( MasterControl master) {
		this.master = master;
	}
	
	/** A Collection of identifiers for all the dockable Frames. */
	public static enum FrameType {
		BAD (""),
		LAYER ("Layers"),
		TOOLS ("Toolset"),
		ANIMATION_SCHEME ("Anim"),
		UNDO("Undo History"),
		;
		
		private String name;
		FrameType( String str) {
			name = str;
		}
		public String getName() {
			return name;
		}
	}
	
	/** The facroty which creates Docked panels based on their type identifier.	 */
	public JPanel createOmniPanel( FrameType type) {
		switch( type) {
		case LAYER:
			return new LayersPanel( master);
		case TOOLS:
			return new PixelSettingsPanel((PixelSettings) master.getToolsetManager().getToolsetSettings(Tool.PEN));
//			return new ToolsPanel(master);
		case ANIMATION_SCHEME:
			return new AnimationSchemePanel(master);
		case UNDO:
			return new UndoPanel(master);
		default:
			return null;
		}
	}
	
	
	// :::: API
	public void performCommand( String command) {
		if( command.equals("showLayerFrame"))
			addFrame( FrameType.LAYER);
		else if( command.equals("showToolsFrame"))
			addFrame( FrameType.TOOLS);
		else if( command.equals("showAnimSchemeFrame"))
			addFrame( FrameType.ANIMATION_SCHEME);
		else if( command.equals("showUndoFrame"))
			addFrame( FrameType.UNDO);
		else if(command.equals("showAnimationView")) {
			JDialog d = new JDialog();
			d.add(new AnimPanel(master));
			d.pack();
			d.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			d.setVisible(true);
		}
		else if( command.equals("showDebugDialog")) {
			DebugDialog d = new DebugDialog(master);
			d.pack();
			d.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			d.setVisible(true);
			
		}
	}
	
	// :::: UI-related
	public void packMainFrame() {
        root = new RootFrame( master);
        root.pack();
        root.setLocationByPlatform(true);
        root.setVisible(true);
	}
	
	/** */
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
		OmniFrame omniFrame = new OmniFrame( master, frameType);
		
		omniFrame.pack();
		
		if( root != null) {
			Point p = root.getLocationOnScreen();
			omniFrame.setLocation( p.x + root.getWidth(), p.y);
		}
		
		omniFrame.setVisible(true);
		omniFrame.addWindowListener(this);
		frames.add(omniFrame);
	}
	
	/** */
	public void showAllFrames() {
		for( OmniFrame frame : frames) {
			frame.toFront();
		}
	}
	
	/***
	 * Constructs a new frame from an already-constructed OmniContainer Panel
	 * 
	 * OmniFrame should be the only one calling this
	 */
	void containerToFrame( OmniContainer container, Point locationOnScreen) {
		OmniFrame frame = new OmniFrame( master, container);
		frame.pack();
		
		// Offset the frame from the mouse coordinates to approximate the tab 
		//	(rather than window top-left) being where you drop it
		//	!!!! could be better if you really want to go through the pain of calculating system metrics
		locationOnScreen.x -= 10;
		locationOnScreen.y -= 40;
		frame.setLocation(locationOnScreen );
		frame.setVisible(true);
		frame.addWindowListener( this);
		frames.add(frame);
	}
	
	
	// :::: WindowListener
	@Override	public void windowClosing(WindowEvent evt) {
		evt.getWindow().removeAll();
		frames.remove(evt.getWindow());
	}
	@Override	public void windowActivated(WindowEvent evt) {}
	@Override	public void windowClosed(WindowEvent evt) {}
	@Override	public void windowDeactivated(WindowEvent evt) {}
	@Override	public void windowDeiconified(WindowEvent evt) {}
	@Override	public void windowIconified(WindowEvent evt) {}
	@Override	public void windowOpened(WindowEvent evt) {}
}
