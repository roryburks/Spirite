package spirite.pc.ui.omni;

import spirite.base.brains.MasterControl;
import spirite.base.brains.commands.CommandExecuter;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.pen.Penner;
import spirite.hybrid.Globals;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;
import spirite.pc.ui.RootFrame;
import spirite.pc.ui.omni.OmniFrame.OmniComponent;
import spirite.pc.ui.omni.OmniFrame.OmniContainer;
import spirite.pc.ui.panel_anim.AnimationPreviewPanel;
import spirite.pc.ui.panel_anim.AnimationSchemePanel;
import spirite.pc.ui.panel_layers.LayersPanel;
import spirite.pc.ui.panel_layers.ReferenceSchemePanel;
import spirite.pc.ui.panel_layers.layer_properties.LayerPropertiesPanel;
import spirite.pc.ui.panel_toolset.ToolSettingsPanel;
import spirite.pc.ui.panel_toolset.UndoPanel;
import spirite.pc.ui.panel_work.WorkPanel;
import spirite.pc.ui.panel_work.WorkPanel.View;
import spirite.pc.ui.panel_work.WorkTabPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;

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

        activeMap = new HashMap<>(FrameType.values().length);
        for( FrameType type : FrameType.values()) 
        	activeMap.put(type, new ArrayList<>());
        
        root = new RootFrame( master, this);
        initCommandMap();
        
	}
	
	/** A Collection of identifiers for all the dockable Frames. */
	public static enum FrameType {
		BAD ("", null),
		LAYER ("Layers","icon.frame.layers"),
		TOOL_SETTINGS ("Tool Settings","icon.frame.toolSettings"),
		ANIMATION_SCHEME ("Anim","icon.frame.animationScheme"),
		UNDO("Undo History","icon.frame.undoHistory"),
		REFERENCE("Reference Scheme","icon.frame.referenceScheme"),
		LAYER_PROPERTIES("Layer Properties",null)
		;
		
		final String name;
		final String icon;
		FrameType( String str, String icon) {
			name = str;
			this.icon = icon;
		}
		public String getName() {
			return name;
		}
		public ImageIcon getIcon() {
			return (icon == null) ? null : Globals.getIcon(icon);
		}
		
		public static FrameType fromString( String string) {
	        for( FrameType check : FrameType.values()) {
	            if( check.name().equals(string))
	            	return check;
	        }
	        return null;
		}
	}
	
	/** The facroty which creates Docked panels based on their type identifier.	 */
	public OmniComponent createOmniComponent( FrameType type) {
		OmniComponent toAdd = null;
		switch( type) {
		case LAYER:
			toAdd = new LayersPanel( master);
			break;
		case TOOL_SETTINGS:
			toAdd = new ToolSettingsPanel( master);
			break;
		case ANIMATION_SCHEME:
			toAdd =  new AnimationSchemePanel(master);
			break;
		case UNDO:
			toAdd = new UndoPanel(master);
			break;
		case REFERENCE:
			toAdd = new ReferenceSchemePanel(master);
			break;
		case BAD:
			break;
		case LAYER_PROPERTIES:
			toAdd = new LayerPropertiesPanel(master);
			break;
		}

		if( toAdd != null)
			activeMap.get(type).add(new WeakReference<>(toAdd));
		
		return toAdd;
	}
	
	private final Map<FrameType, List<WeakReference<OmniComponent>>> activeMap;
	
	
	
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
//		ret.plus(root.getOmniFrame);
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
		
		// Really should be an easier way to plus events to the queue without
		//	processing them immediately, but I'd rather invoke SwingUtilities
		//	than get the Default Toolkit
		for( OmniDialog d : dialogs) {
			if( d.frame == frame) {
				SwingUtilities.invokeLater( () -> {
					d.dispatchEvent( new WindowEvent(d,WindowEvent.WINDOW_CLOSING));
				});
			}
		}
	}
	
	public View getZoomerForWorkspace( ImageWorkspace ws) {
		if( root == null) return null;
		
		WorkTabPane wsPane = root.getWTPane();
		return wsPane.getZoomerForWorkspace(ws);
	}
//	public void setBottomBarMessage(String s) {
//		root.getWTPane().set
//	}
	
	public Penner getPenner() {

		if( root == null) return null;
		
		WorkTabPane wsPane = root.getWTPane();
		return wsPane.getPenner();
	}
	public WorkPanel getWorkPanel() {
		return root.getWTPane().workPanel;
	}
	
	// ======= 
	// ==== Component Tracking
	
	// :: WindowListener
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
		ArrayList<String> entries = new ArrayList<>( commandMap.size() + FrameType.values().length);
		
		entries.addAll(commandMap.keySet());
		
		for( FrameType type : FrameType.values()) 
			entries.add( "focus." + type.toString());
		
		return entries;
	}

	@Override
	public String getCommandDomain() {
		return "frame";
	}

	@Override
	public boolean executeCommand(String command, Object extra) {
		int dot = command.indexOf('.');
		
		if( dot != -1) {
			FrameType type = FrameType.fromString(command.substring(dot+1));
			
			if( type == null)
				return false;
				
			switch( command.substring(0, dot)) {
			case "focus":
				focusFrame( type);
				return true;
			}
		}
		
		Runnable runnable = commandMap.get(command);
		if( runnable != null) {
			runnable.run();
			return true;
		}
		return false;
		
	}

	/** Requests focus for the first frame of the given type. */
	public void focusFrame(FrameType type) {
		if( type == null) return;
		
		boolean done = false;
		Iterator<WeakReference<OmniComponent>> it = activeMap.get(type).iterator();
		while( it.hasNext()) {
			WeakReference<OmniComponent> ref = it.next();
			
			if( ref.get() == null)
				it.remove();
			else if(!done) {
				ref.get().getComponent().requestFocus();
				done = true;
			}
		}
	}
}
