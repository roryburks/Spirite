package spirite.pc.ui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.CommandExecuter;
import spirite.base.image_data.ImageWorkspace;
import spirite.hybrid.Globals;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;
import spirite.pc.pen.Penner;
import spirite.pc.ui.OmniFrame.OmniComponent;
import spirite.pc.ui.OmniFrame.OmniContainer;
import spirite.pc.ui.panel_anim.AnimationPreviewPanel;
import spirite.pc.ui.panel_anim.AnimationSchemePanel;
import spirite.pc.ui.panel_layers.LayersPanel;
import spirite.pc.ui.panel_layers.ReferenceSchemePanel;
import spirite.pc.ui.panel_toolset.ToolSettingsPanel;
import spirite.pc.ui.panel_toolset.UndoPanel;
import spirite.pc.ui.panel_work.WorkPanel;
import spirite.pc.ui.panel_work.WorkTabPane;
import spirite.pc.ui.panel_work.WorkPanel.View;

public class FrameManager 
	implements WindowListener, CommandExecuter
{
	private final MasterControl master;
	
	private final List<OmniDialog> dialogs = new ArrayList<>();
	private final RootFrame root;
	private class OmniDialog extends JDialog {
		OmniFrame frame;
	}

	public FrameManager( MasterControl master) {
		this.master = master;

        root = new RootFrame( master);
        initCommandMap();
	}
	
	/** A Collection of identifiers for all the dockable Frames. */
	public static enum FrameType {
		BAD (""),
		LAYER ("Layers"),
		TOOL_SETTINGS ("Tool Settings"),
		ANIMATION_SCHEME ("Anim"),
		UNDO("Undo History"),
		REFERENCE("Reference Scheme"),
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
	public OmniComponent createOmniComponent( FrameType type) {
		switch( type) {
		case LAYER:
			return new LayersPanel( master);
		case TOOL_SETTINGS:
			return new ToolSettingsPanel( master);
//			return new ToolsPanel(master);
		case ANIMATION_SCHEME:
			return new AnimationSchemePanel(master);
		case UNDO:
			return new UndoPanel(master);
		case REFERENCE:
			return new ReferenceSchemePanel(master);
		case BAD:
			break;
		}
		
		return null;
	}

	public static ImageIcon getIconForType( FrameType type) {
		switch( type) {
		case LAYER:
			return Globals.getIcon("icon.frame.layers");
		case TOOL_SETTINGS:
			return Globals.getIcon("icon.frame.toolSettings");
		case ANIMATION_SCHEME:
			return Globals.getIcon("icon.frame.animationScheme");
		case UNDO:
			return Globals.getIcon("icon.frame.undoHistory");
		case REFERENCE:
			return Globals.getIcon("icon.frame.referenceScheme");
		case BAD:
			return null;
		}
		return null;
	}
	
	
	
	// :::: UI-related
	public void packMainFrame() {
        root.pack();
        root.setLocationByPlatform(true);
        root.setVisible(true);
	}
	
	public RootFrame getRootFrame() {
		return root;
	}
	
	private List<OmniFrame> frameList() {
		List<OmniFrame> ret = new ArrayList<>(dialogs.size()+1);
		for( OmniDialog d : dialogs) {
			ret.add(d.frame);
		}
//		ret.add(root.getOmniFrame);
		return ret;
	}
	
	/** */
	public void addFrame( FrameType frameType) {
		// First Check to make sure the frame type isn't already open
		//	(assuming it's not duplicateable)
		
		for( OmniDialog d : dialogs) {
			if( d.frame.containsFrameType( frameType)) {
				d.toFront();
				
				return;
			}
		}
		
		// Next create the container frame and show it
		OmniDialog d = new OmniDialog();
		OmniFrame omniFrame = new OmniFrame( master, frameType);
		d.frame = omniFrame;
		d.add(omniFrame);
		d.pack();
		
		if( root != null) {
			Point p = root.getLocationOnScreen();
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			int put_x = Math.min( screenSize.width - d.getWidth(), p.x + root.getWidth());
			d.setLocation( put_x, p.y);
		}

		d.setVisible(true);
		d.addWindowListener(this);
		dialogs.add(d);
	}
	
	/** */
	public void showAllFrames() {
		for( OmniDialog d: dialogs) {
			d.toFront();
		}
	}
	
	/***
	 * Constructs a new frame from an already-constructed OmniContainer Panel
	 * 
	 * OmniFrame should be the only one calling this
	 */
	void containerToFrame( OmniContainer container, Point locationOnScreen) {
		OmniDialog d = new OmniDialog();
		OmniFrame frame = new OmniFrame( master, container);
		d.frame = frame;
		d.add(frame);
		d.pack();
		
		// Offset the frame from the mouse coordinates to approximate the tab 
		//	(rather than window top-left) being where you drop it
		//	!!!! could be better if you really want to go through the pain of calculating system metrics
		locationOnScreen.x -= 10;
		locationOnScreen.y -= 40;
		d.setLocation(locationOnScreen );
		
		d.setVisible(true);
		d.addWindowListener( this);
		dialogs.add(d);
	}
	
	/** When an OmniFrame is made empty from internal mechanisms, it will call this. */
	void triggereClose(OmniFrame frame) {
		
		// Really should be an easier way to add events to the queue without
		//	processing them immediately, but I'd rather invoke SwingUtilities
		//	than get the Default Toolkit
		for( OmniDialog d : dialogs) {
			if( d.frame == frame) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						d.dispatchEvent( new WindowEvent(d,WindowEvent.WINDOW_CLOSING));
					}
				});
			}
		}
	}
	
	public View getZoomerForWorkspace( ImageWorkspace ws) {
		if( root == null) return null;
		
		WorkTabPane wsPane = root.getWTPane();
		return wsPane.getZoomerForWorkspace(ws);
	}
	public Penner getPenner() {

		if( root == null) return null;
		
		WorkTabPane wsPane = root.getWTPane();
		return wsPane.getPenner();
	}
	public WorkPanel getWorkPanel() {
		return root.getWTPane().workPanel;
	}
	
	// :::: WindowListener
	@Override	public void windowClosing(WindowEvent evt) {
		if( !dialogs.contains(evt.getWindow())) {
			MDebug.handleError(ErrorType.STRUCTURAL_MINOR, null, "Unknown Dialog Closing in Frame Manager");
		}
		else {
			OmniDialog d = (OmniDialog)evt.getWindow();
			evt.getWindow().removeAll();	// Done to try to stop leak described in OmniFrame.java
			
			d.frame.triggerCleanup();
			d.dispose();
			dialogs.remove(d);
		}
	}
	@Override	public void windowActivated(WindowEvent evt) {}
	@Override	public void windowClosed(WindowEvent evt) {}
	@Override	public void windowDeactivated(WindowEvent evt) {}
	@Override	public void windowDeiconified(WindowEvent evt) {}
	@Override	public void windowIconified(WindowEvent evt) {}
	@Override	public void windowOpened(WindowEvent evt) {}

	// :::: CommandExecuter
	private final Map<String,Runnable> commandMap = new HashMap<>();
	
	private void initCommandMap() {
		commandMap.put("showLayerFrame", new Runnable() {
			@Override public void run() {
				addFrame( FrameType.LAYER);
			}
		});commandMap.put("showToolsFrame", new Runnable() {
			@Override public void run() {
				addFrame( FrameType.TOOL_SETTINGS);
			}
		});commandMap.put("showAnimSchemeFrame", new Runnable() {
			@Override public void run() {
				addFrame( FrameType.ANIMATION_SCHEME);
			}
		});commandMap.put("showUndoFrame", new Runnable() {
			@Override public void run() {
				addFrame( FrameType.UNDO);
			}
		});commandMap.put("showReferenceFrame", new Runnable() {
			@Override public void run() {
				addFrame( FrameType.REFERENCE);
			}
		});commandMap.put("showAnimationView", new Runnable() {
			@Override public void run() {
				JDialog d = new JDialog();
				d.add(new AnimationPreviewPanel(master));
				d.pack();
				d.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				d.setVisible(true);
			}
		});
	}
	
	@Override
	public List<String> getValidCommands() {
		return new ArrayList<>(commandMap.keySet());
	}

	@Override
	public String getCommandDomain() {
		return "frame";
	}

	@Override
	public boolean executeCommand(String command) {
		Runnable runnable = commandMap.get(command);
		
		if( runnable != null) {
			runnable.run();
			return true;
		}
		return false;
	}
}
