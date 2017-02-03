
package spirite.ui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import spirite.MUtil.TransferableImage;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.CommandExecuter;
import spirite.brains.RenderEngine.RenderSettings;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageWorkspace;
import spirite.panel_layers.LayersPanel;
import spirite.panel_layers.ReferenceSchemePanel;
import spirite.panel_layers.RigPanel;
import spirite.panel_toolset.ColorPicker;
import spirite.panel_toolset.PalettePanel;
import spirite.panel_toolset.ToolSettingsPanel;
import spirite.panel_toolset.ToolsPanel;
import spirite.panel_work.WorkPanel;
import spirite.panel_work.WorkPanel.Zoomer;
import spirite.panel_work.WorkTabPane;
import spirite.ui.components.ResizeContainerPanel;
import spirite.ui.components.ResizeContainerPanel.ContainerOrientation;

/**
 * While the MasterControl is "home base" for all the internals of the program, the root
 * frame is home base for the UI.  In addition to containing the menu bar and whatever
 * panels are attached to it, it also is the delegator for all of the Hotkeys.
 */
public class RootFrame extends javax.swing.JFrame
        implements KeyEventDispatcher, WindowFocusListener, ActionListener, 
        	WindowListener, CommandExecuter
{
	private static final long serialVersionUID = 1L;
    
    private final MasterControl master;

    public RootFrame( MasterControl master) {
        this.master =  master;
        
        initComponents();
        
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.addWindowListener(this);
        this.addWindowFocusListener( this);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
        

        master.newWorkspace(640,480,new java.awt.Color(0,0,0,0), true);
        master.getCurrentWorkspace().finishBuilding();
    }
    

    private PalettePanel palettePanel;
    private ToolsPanel toolsPanel;
    private ToolSettingsPanel settingPanel;
    private WorkTabPane workPane;
    private RigPanel rigPanel;
    
    private JPanel leftContainer;
    private ResizeContainerPanel rightContainer;
    private ResizeContainerPanel rrContainer;
    
    private ResizeContainerPanel container;

    

    private void initComponents() {
    	this.setLayout(new GridLayout());
    	
    	workPane = new WorkTabPane( master);
    	toolsPanel = new ToolsPanel( master);
    	palettePanel = new PalettePanel( master);
    	settingPanel = new ToolSettingsPanel( master);
    	rigPanel = new RigPanel(master);
    	ReferenceSchemePanel rsp =  new ReferenceSchemePanel(master);
    	
    	leftContainer = new JPanel();
    	rightContainer = new ResizeContainerPanel(palettePanel, ContainerOrientation.VERTICAL);
    	rrContainer = new ResizeContainerPanel(rsp, ContainerOrientation.VERTICAL);
    	
    	workPane.setPreferredSize(new Dimension(800,600));
    	container = new ResizeContainerPanel(workPane,ContainerOrientation.HORIZONTAL);
    	this.add(container);

    	container.addPanel(50, 240, 1, leftContainer);
    	container.addPanel(50, 160, -1, rightContainer);
    	container.addPanel(50, 160, -2, rrContainer);
    	container.setStretchArea(200);

    	rightContainer.addPanel(50, 80, 0, toolsPanel);
    	rightContainer.addPanel(50, 160, 0, settingPanel);
    	rightContainer.addPanel(50, 160, 0, new ColorPicker());
    	rightContainer.setStretchArea(80);
    	
    	rrContainer.addPanel( 100, 300, -1, rigPanel);
    	rrContainer.setStretchArea(100);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        workPane.setMinimumSize(new java.awt.Dimension(300, 300));
        toolsPanel.setPreferredSize(new java.awt.Dimension(140, 80));

        initMenu();

        // LeftPanel Layout
        leftContainer.setLayout(new GridLayout());
        leftContainer.add(new LayersPanel(master));
        
        pack();
    }                   
    



    

    private void initMenu() {
    	final String[][] menuScheme = {
    			//Name, actionString, icon
    			{"&File", null, null},
    			{".&New Image", "global.new_image", null},
    			{".-"},
    			{".&Open", "global.open_image", null},
    			{".-"},
    			{".&Save Workspace", "global.save_image", null},
    			{".Save Workspace &As...", "global.save_image_as", null},
    			{".-"},
    			{".Export Image", "global.export", null},
    			{".Export Image As...", "global.export_as", null},
    			{".-"},
    			{".Debug &Color", "global.debug_color", null},
    			
    			{"&Edit", null, null},
    			{".&Undo", "draw.undo", null},
    			{".&Redo", "draw.redo", null},
    			
    			
    			{"&Layer", null, null},
    			{".Auto&crop Layer", "draw.autocroplayer", null},
    			
    			{"&Window", null, null},
    			{".&Dialogs", null, null},
    			{"..&Layers", "frame.showLayerFrame", "icon.frame.layers"},
    			{"..&Tools", "frame.showToolsFrame", "icon.frame.toolSettings"},
    			{"..-"},
    			{"..Animation &Scheme", "frame.showAnimSchemeFrame", "icon.frame.animationScheme"},
    			{"..Undo &History", "frame.showUndoFrame", "icon.frame.undoHistory"},
    			{"..&Reference Scheme", "frame.showReferenceFrame", "icon.frame.referenceScheme"},
    			
    			{".&Animation View", "frame.showAnimationView", null},
    			{".Debug View", "frame.showDebugDialog", null},
    			
    			{"&Settings", null, null},
    			{".Manage &Hotkeys", "dialog.HOTKEY", null},
    	};
    	
    	JMenuBar jMenuBar = new JMenuBar();
    	UIUtil.constructMenu(jMenuBar, menuScheme, this);
    	
    	
        setJMenuBar(jMenuBar);
    }
    
    // :::: KeyEventDispatcher
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {


        // Hotkeys
        if( e.getID() == KeyEvent.KEY_PRESSED) {
            int key = e.getKeyCode();
            int modifier = e.getModifiersEx();
            
        	// ::: Copy/Cut/Paste have hard-coded hotkeys
//			int mod = e.getModifiersEx() & (KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);
//			if( modifier == mod) {	// I'm not sure why I was compelled to limit it to no extended modifiers, but I don't think there's a good reason

            String command = master.getHotekyManager().getCommand( key, modifier);

        	if( command != null)
        		master.executeCommandString(command);

        }
        
        return false;
    }
    

    private void copy() {
    	ImageWorkspace workspace = master.getCurrentWorkspace();
    	
    	if( workspace == null) return;
    	
    	RenderSettings settings = new RenderSettings();
    	settings.workspace = workspace;
    	
    	GroupTree.Node node = workspace.getSelectedNode();
    	
    	if( node instanceof GroupTree.LayerNode) 
    		settings.layer = ((GroupTree.LayerNode)node).getLayer();
    	else if( node instanceof GroupTree.GroupNode) 
    		settings.node = (GroupTree.GroupNode)node;

    	BufferedImage img = master.getRenderEngine().renderImage(settings);
    	
    	TransferableImage transfer = new TransferableImage(img);

    	Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
    	c.setContents(transfer, null);
    }

    // :::: WindowFocusListener
	@Override	public void windowLostFocus(WindowEvent arg0) {	}
	@Override
	public void windowGainedFocus(WindowEvent evt) {
		// !!!! TODO : This causes issues when you move a window to a different Desktop/Workspace
		//	but solving this problem in pure Java will either be hacky or impossible.
		
		if( evt.getOppositeWindow() == null) {
			master.getFrameManager().showAllFrames();
			this.toFront();
		}
	}


	// :::: ActionListener
	@Override
	public void actionPerformed(ActionEvent evt) {
		master.executeCommandString(evt.getActionCommand());
	}

	// :::: WindowListener
	@Override	public void windowActivated(WindowEvent arg0) {	}
	@Override	public void windowClosed(WindowEvent arg0) {}
	@Override	public void windowDeactivated(WindowEvent arg0) {}
	@Override	public void windowDeiconified(WindowEvent arg0) {}
	@Override	public void windowIconified(WindowEvent arg0) {}
	@Override	public void windowOpened(WindowEvent arg0) {}
	@Override
	public void windowClosing(WindowEvent evt) {
		int unsaved = 0;
		List<ImageWorkspace> workspaces = master.getWorkspaces();
		
		for( ImageWorkspace workspace : workspaces) {
			if( workspace.hasChanged())
				unsaved++;
		}
		
		if( unsaved > 1) {
			int response = JOptionPane.showConfirmDialog(this, 
					"Multiple Workspaces are unsaved, save them before closing?", 
					"Closing Program",
					JOptionPane.YES_NO_CANCEL_OPTION);
			
			 if( response == JOptionPane.CANCEL_OPTION)
				return;
			 else if( response == JOptionPane.YES_OPTION) {
				 for( ImageWorkspace workspace : workspaces) {
					 if( workspace.hasChanged()) {
						 // Prompt User to save the workspace, 
						 if( master.promptSave(workspace) == JOptionPane.CANCEL_OPTION)
							 return;
					 }
				 }
			 }
		}
		else if (unsaved == 1) {
			 for( ImageWorkspace workspace : workspaces) {
				 if( workspace.hasChanged()) {
					 // Prompt User to save the workspace, 
					 if( master.promptSave(workspace) == JOptionPane.CANCEL_OPTION)
						 return;
				 }
			 }
			 this.dispose();
			
		}
		
		
		this.dispose();
        System.exit(0);
	}



	// CommandExecuter
	@Override
	public List<String> getValidCommands() {
		// TODO: Less hard-coded.
		return Arrays.asList(new String[]{
			"zoom_in",
			"zoom_out",
			"zoom_in_slow",
			"zoom_out_slow",
			"zoom_0"
		});
	}

	@Override
	public String getCommandDomain() {
		return "context";
	}
	
	@Override
	public boolean executeCommand(String command) {
    	// !!! TODO: As I add new components that can have contextual commands
    	//	figure out how I want to generalize this
    	WorkPanel workPanel = workPane.getCurrentWorkPane();
    	if( workPanel == null) return true;
    	
    	Zoomer zoomer = (workPanel.workspace.getReferenceManager().isEditingReference())?
    			workPanel.refzoomer : workPanel.zoomer;
    	
        if( command.equals("zoom_in")) {
        	zoomer.zoomIn();
        	return true;
        }
        else if( command.equals("zoom_out")) {
        	zoomer.zoomOut();
        	return true;
        }
        else if( command.equals("zoom_in_slow")) {
        	zoomer.setZoomLevel(zoomer.getZoomLevel()+1);
        	return true;
        }
        else if( command.equals("zoom_out_slow")) {
        	zoomer.setZoomLevel(zoomer.getZoomLevel()-1);
        	return true;
        }
        else if( command.equals("zoom_0")) {
        	zoomer.setZoomLevel(0);
        	return true;
        }
	    
		return false;
	}
}
