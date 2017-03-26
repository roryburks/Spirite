
package spirite.ui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import spirite.brains.MasterControl;
import spirite.brains.MasterControl.CommandExecuter;
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
    	rightContainer.addPanel(160, 160, 0, new ColorPicker(master));
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
    			
    			{"&Edit", null, null},
    			{".&Undo", "draw.undo", null},
    			{".&Redo", "draw.redo", null},
    			
    			
    			{"&Layer", null, null},
    			{".Auto&crop Layer", "draw.autocroplayer", null},
    			{".Layer to &Image Size", "draw.layerToImageSize", null},
    			
    			{"&Select", null, null},
    			{".&All", "select.all", null},
    			{".&None", "select.none", null},
    			{".&Invert Selection (unimplemented)", "select.invert", null},
    			
    			{"&Image", null, null},
    			{".&Invert", "draw.invert", null},
    			{".&To Color", "draw.toColor", null},
    			
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
    
    WorkTabPane getWTPane() {
    	return this.workPane;
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
			
		}
		


		// If the SaveEngine is locked (still saving a file), wait 10 seconds,
		//	if it's still saving, prompt the user to override, wait 30 more seconds,
		//	then repeat (30 seconds -> prompt ->).
		//
		// Thread-inside-thread format so that the user can still experience UI 
		//	Feedback while thread is waiting for SaveEngine to unlock
		final int cursorType = Cursor.WAIT_CURSOR;
		final Component glassPane = getGlassPane();
		glassPane.setCursor(Cursor.getPredefinedCursor(cursorType));
		glassPane.setVisible(true);
		
		Thread outer = new Thread( new Runnable() {@Override public void run() {
			Thread thread = new Thread(new Runnable() {@Override public void run() {
				while( master.getSaveEngine().isLocked() ) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}});
			try {
				int option = JOptionPane.NO_OPTION;
				thread.start();
				thread.join(10*1000);
				while( master.getSaveEngine().isLocked() && 
						thread.isAlive() &&  option == JOptionPane.NO_OPTION)
				{
					thread.join(30*1000);
					option = JOptionPane.showConfirmDialog(null, "File is still saving, close anyway?", "Closing Spirite", JOptionPane.YES_NO_CANCEL_OPTION);
				}
				
				if( option == JOptionPane.CANCEL_OPTION) {
					SwingUtilities.invokeLater(new Runnable() {@Override public void run() {
						glassPane.setVisible(false);
					}});
					return;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			SwingUtilities.invokeLater(new Runnable() {@Override public void run() {
				dispose();
		        System.exit(0);	
			}});
		}});


		outer.start();
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
    	
    	Zoomer zoomer = workPanel.zoomer;
    	
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
