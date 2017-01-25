
package spirite.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JColorChooser;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import spirite.MDebug;
import spirite.MUtil.TransferableImage;
import spirite.brains.MasterControl;
import spirite.dialogs.Dialogs;
import spirite.dialogs.NewImagePanel;
import spirite.file.LoadEngine;
import spirite.file.LoadEngine.BadSIFFFileException;
import spirite.file.SaveEngine;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.RenderEngine.RenderSettings;
import spirite.panel_layers.LayersPanel;
import spirite.panel_layers.ReferenceSchemePanel;
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
        implements KeyEventDispatcher, WindowFocusListener, ActionListener, WindowListener
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
    
    private JPanel leftContainer;
    private ResizeContainerPanel rightContainer;
    private JPanel rrContainer;
    
    private ResizeContainerPanel container;

    

    private void initComponents() {
    	this.setLayout(new GridLayout());
    	
    	workPane = new WorkTabPane( master);
    	toolsPanel = new ToolsPanel( master);
    	palettePanel = new PalettePanel( master);
    	settingPanel = new ToolSettingsPanel( master.getToolsetManager());
    	
    	leftContainer = new JPanel();
    	rightContainer = new ResizeContainerPanel(palettePanel, ContainerOrientation.VERTICAL);
    	rrContainer = new JPanel();
    	
    	workPane.setPreferredSize(new Dimension(800,600));
    	container = new ResizeContainerPanel(workPane,ContainerOrientation.HORIZONTAL);
    	this.add(container);

    	container.addPanel(50, 160, 1, leftContainer);
    	container.addPanel(50, 160, -1, rightContainer);
    	container.addPanel(50, 160, -2, rrContainer);
    	container.setStretchArea(200);

    	rightContainer.addPanel(50, 80, 0, toolsPanel);
    	rightContainer.addPanel(50, 160, 0, settingPanel);
    	rightContainer.setStretchArea(80);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        workPane.setMinimumSize(new java.awt.Dimension(300, 300));
        toolsPanel.setPreferredSize(new java.awt.Dimension(140, 80));

        initMenu();

        // LeftPanel Layout
        leftContainer.setLayout(new GridLayout());
        leftContainer.add(new LayersPanel(master));
        rrContainer.setLayout(new GridLayout());
        rrContainer.add( new ReferenceSchemePanel(master));
        
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
    			{".&Undo", "global.undo", null},
    			{".&Redo", "global.redo", null},
    			
    			{"&Window", null, null},
    			{".&Dialogs", null, null},
    			{"..&Layers", "frame.showLayerFrame", "icon.frame.layers"},
    			{"..&Tools", "frame.showToolsFrame", "icon.frame.toolSettings"},
    			{"..-"},
    			{"..Animation &Scheme", "frame.showAnimSchemeFrame", "icon.frame.animationScheme"},
    			{"..Undo &History", "frame.showUndoFrame", "icon.frame.undoHistory"},
    			{"..&Reference Scheme", "frame.showReferenceFrame", "icon.frame.referenceScheme"},
    			
    			{".&Animation View", "frame.showAnimationView", null},
    			{".Debug View", "frame.showDebugDialog", null}
    	};
    	
    	JMenuBar jMenuBar = new JMenuBar();
    	UIUtil.constructMenu(jMenuBar, menuScheme, this);
    	
    	
        setJMenuBar(jMenuBar);
    }
    
    // :::: Menu Actions
    
    /** Prompts for a new image dialog and then performs it. */
    private void promptNewImage() {     
        NewImagePanel panel = new NewImagePanel(master);

        int response = JOptionPane.showConfirmDialog(this,
                panel,
                "New Image",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if( response == JOptionPane.OK_OPTION) {
            master.newWorkspace(panel.getValueWidth(), panel.getValueHeight(), panel.getValueColor(), true);
            master.getCurrentWorkspace().finishBuilding();
        }
    }                                            

    /**     */
    private void promptDebugColor() {
        // TODO DEBUG
        JColorChooser jcp = new JColorChooser();
        int response = JOptionPane.showConfirmDialog(this, jcp, "Choose Color", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if( response == JOptionPane.OK_OPTION) {
            master.getPaletteManager().setActiveColor(0, jcp.getColor());
        }

    }                                             



    /**
     * Performs the given hotkey command string or delegates the command to the
     * appropriate component
     */
    public void performCommand( String command) {
        if( command != null) {
            if( command.startsWith("global."))
                globalHotkeyCommand(command.substring("global.".length()));
            else if( command.startsWith("toolset."))
                master.getToolsetManager().setSelectedTool(command.substring("toolset.".length()));
            else if( command.startsWith("palette."))
            	master.getPaletteManager().performCommand(command.substring("palette.".length()));
            else if( command.startsWith("frame."))
            	master.getFrameManager().performCommand(command.substring("frame.".length()));
            else if( command.startsWith("context."))
                contextualCommand(command.substring("context.".length()));
            else if( command.startsWith("draw.")){
            	final ImageWorkspace workspace = master.getCurrentWorkspace();
            	if( workspace != null) {
            		workspace.executeDrawCommand( command.substring("draw.".length()));
            	}
            }
            else {
            	MDebug.handleWarning( MDebug.WarningType.REFERENCE, this, "Unknown Command String prefix: " + command);
            }
        }
    }
    
    private void contextualCommand( String command) {
    	// !!! TODO: As I add new components that can have contextual commands
    	//	figure out how I want to generalize this
    	WorkPanel workPanel = workPane.getCurrentWorkPane();
    	if( workPanel == null) return;
    	
    	Zoomer zoomer = (workPanel.workspace.isEditingReference())?
    			workPanel.refzoomer : workPanel.zoomer;
    	
        if( command.equals("zoom_in")) {
        	zoomer.zoomIn();
        }
        else if( command.equals("zoom_out")) {
        	zoomer.zoomOut();
        }
        else if( command.equals("zoom_in_slow")) {
        	zoomer.setZoomLevel(zoomer.getZoomLevel()+1);
        }
        else if( command.equals("zoom_out_slow")) {
        	zoomer.setZoomLevel(zoomer.getZoomLevel()-1);
        }
        else if( command.equals("zoom_0")) {
        	zoomer.setZoomLevel(0);
        }
        else {
        	MDebug.handleWarning( MDebug.WarningType.REFERENCE, this, "Unknown contextual command: context." + command);
        }
    }
    
    /** Performs the given hotkey command string (should be of "global." focus). */
    private void globalHotkeyCommand( String command) {
        if( command.equals("new_image"))
        	promptNewImage();
        else if( command.equals("toggle_reference")) {
        	ImageWorkspace workspace = master.getCurrentWorkspace();
        	if( workspace != null) {
        		workspace.setEditingReference(!workspace.isEditingReference());
        	}
        }
        else if( command.equals("debug_color"))
        	promptDebugColor();
        else if( command.equals("newLayerQuick")) {
        	ImageWorkspace workspace = master.getCurrentWorkspace();
        	if( workspace != null) {
        		workspace.setSelectedNode(
        			workspace.addNewSimpleLayer( 
        				workspace.getSelectedNode(), 
        				workspace.getWidth(), 
        				workspace.getHeight(), 
        				"newLayer", 
        				new Color(0,0,0,0)));
        		
        		
        	}
        }
        else if( command.equals("open_image")){
			File f =Dialogs.pickFileOpen();
			
			if( f != null) {
	        	openFile( f);
			}
        }
        else if( command.equals("save_image")) {
        	ImageWorkspace workspace = master.getCurrentWorkspace();

        	File f=workspace.getFile();

        	if( workspace.hasChanged() || f == null) {
	        	if( f == null)
	        		f = Dialogs.pickFileSave();
	        	
	        	if( f != null) {
					SaveEngine.saveWorkspace( workspace, f);
					workspace.fileSaved(f);
					master.getSettingsManager().setWorkspaceFilePath(f);
	        	}
        	}
        }
		else if( command.equals("save_image_as")) {
			File f = Dialogs.pickFileSave();
			
			if( f != null)
				SaveEngine.saveWorkspace( master.getCurrentWorkspace(), f);
		}
		else if( command.equals("undo")) {
			if( master.getCurrentWorkspace() != null)
				master.getCurrentWorkspace().getUndoEngine().undo();
		}
		else if( command.equals("redo")) {
			if( master.getCurrentWorkspace() != null)
				master.getCurrentWorkspace().getUndoEngine().redo();
		}
		else if( command.equals("export") || command.equals("export_as")) {
			File f = Dialogs.pickFileExport();
			
			if( f != null) {
				exportWorkspaceToFile( master.getCurrentWorkspace(), f);
			}
		}
        else {
        	MDebug.handleWarning( MDebug.WarningType.REFERENCE, this, "Unknown global command: global." + command);
        }
    }
    
    private void openFile( File f) {
    	String ext = f.getName().substring( f.getName().lastIndexOf(".")+1);
    	
    	boolean attempted = false;
    	if( ext.equals("png") || ext.equals("bmp") || ext.equals("jpg") || ext.equals("jpeg")) 
    	{
    		// First try to load the file as normal if it's a normal image format
    		try {
				BufferedImage bi = ImageIO.read(f);
				master.createWorkspaceFromImage(bi, true).fileSaved(f);
				master.getSettingsManager().setImageFilePath(f);
				return;
			} catch (IOException e) {
				attempted = true;
			}
    	}
		try {
			// If it's not recognized (or failed to load) as a normal file, try to
			//	load it as an SIF
			ImageWorkspace ws = master.addWorkpace( 
				LoadEngine.loadWorkspace( f), true);
			ws.fileSaved(f);
			master.getSettingsManager().setWorkspaceFilePath(f);
			return;
		} catch (BadSIFFFileException e) {}
		if( !attempted) {
			// If we didn't try to load the image as a normal format already (if 
			//	its extension was not recognized) and loading it as an SIF failed,
			//	try to load it as a normal Image
    		try {
				BufferedImage bi = ImageIO.read(f);
				master.createWorkspaceFromImage(bi, true).fileSaved(f);
				master.getSettingsManager().setImageFilePath(f);
				return;
			} catch (IOException e) {}
		}
    }
    
    private void exportWorkspaceToFile( ImageWorkspace workspace, File f) {
    	String ext = f.getName().substring( f.getName().lastIndexOf(".")+1);
    	
    	RenderSettings settings = new RenderSettings();
    	settings.workspace = workspace;
    	BufferedImage bi = master.getRenderEngine().renderImage(settings);
    	
    	if( ext.equals("jpg") || ext.equals("jpeg")) {
    		// Remove Alpha Layer of JPG so that encoding works correctly
    		BufferedImage bi2 = bi;
    		bi = new BufferedImage( bi2.getWidth(), bi2.getHeight(), BufferedImage.TYPE_INT_RGB);
    		Graphics g = bi.getGraphics();
    		g.drawImage(bi2, 0, 0, null);
    		g.dispose();
    	}
    	
    	try {
			ImageIO.write( bi, ext, f);
			master.getSettingsManager().setImageFilePath(f);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Failed to Export file: " + e.getMessage());
			e.printStackTrace();
		}
    }
    
    // :::: KeyEventDispatcher
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int mod = e.getModifiersEx() & (KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);


        // Hotkeys
        if( e.getID() == KeyEvent.KEY_PRESSED) {
            int key = e.getKeyCode();
            int modifier = e.getModifiersEx();
            
        	// ::: Copy/Cut/Paste have hard-coded hotkeys
            if( mod == KeyEvent.CTRL_DOWN_MASK && e.getKeyCode() == KeyEvent.VK_V) 
            	paste();
            else if( mod == KeyEvent.CTRL_DOWN_MASK && e.getKeyCode() == KeyEvent.VK_C)
            	copy();
            else if( modifier == mod) {
            	String command = master.getHotekyManager().getCommand( key, modifier);

            	performCommand(command);
            }

        }
        
        return false;
    }
    
    private void paste() {
    	Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
    	
    	try {
    		// Paste Clipboard to new SimpleLayer
    		Image img = (Image) c.getData(DataFlavor.imageFlavor);
    		int width = img.getWidth(null);
    		int height = img.getHeight(null);
    		boolean newWorkspace = false;

    		ImageWorkspace workspace = master.getCurrentWorkspace();
    		if( workspace == null) {
    			workspace = new ImageWorkspace(master.getCacheManager());
    			newWorkspace = true;
    		}
    		
    		BufferedImage bi = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB);
    		Graphics g = bi.getGraphics();
    		g.drawImage(img, 0, 0, null);
    		workspace.addNewSimpleLayer( workspace.getSelectedNode(), bi, "Pasted Layer");

    		if( newWorkspace)
    			workspace.finishBuilding();
    	} catch( Exception e) {}
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
		performCommand(evt.getActionCommand());
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

	
	
}
