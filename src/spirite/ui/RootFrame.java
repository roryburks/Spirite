
package spirite.ui;

import java.awt.Color;
import java.awt.Graphics;
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

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MUtil.TransferableImage;
import spirite.brains.MasterControl;
import spirite.brains.ToolsetManager.ToolSettings;
import spirite.brains.ToolsetManager.ToolsetSettingsPanel;
import spirite.dialogs.Dialogs;
import spirite.dialogs.NewImagePanel;
import spirite.file.LoadEngine;
import spirite.file.LoadEngine.BadSIFFFileException;
import spirite.file.SaveEngine;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.RenderEngine.RenderSettings;
import spirite.panel_toolset.PalettePanel;
import spirite.panel_toolset.ToolSettingsPanel;
import spirite.panel_toolset.ToolsPanel;
import spirite.panel_work.WorkPanel;
import spirite.panel_work.WorkTabPane;

/**
 * While the MasterControl is "home base" for all the internals of the program, the root
 * frame is home base for the UI.  In addition to containing the menu bar and whatever
 * panels are attached to it, it also is the delegator for all of the Hotkeys.
 */
public class RootFrame extends javax.swing.JFrame
        implements KeyEventDispatcher, WindowFocusListener, ActionListener, WindowListener
{
	private static final long serialVersionUID = 1L;
    private PalettePanel palettePanel;
    private ToolsPanel toolsPanel;
    private ToolSettingsPanel settingPanel;
    private WorkTabPane workPane;
    
    private final MasterControl master;

    public RootFrame( MasterControl master) {
        this.master =  master;
        
        initComponents();
        
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.addWindowListener(this);
        this.addWindowFocusListener( this);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
        

        master.newWorkspace(128,128,new java.awt.Color(0,0,0,0), true);
        master.getCurrentWorkspace().finishBuilding();
    }

    private void initComponents() {
    	workPane = new WorkTabPane( master);
    	toolsPanel = new ToolsPanel( master);
    	palettePanel = new PalettePanel( master);
    	settingPanel = new ToolSettingsPanel( master.getToolsetManager());

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        workPane.setMinimumSize(new java.awt.Dimension(300, 300));
        toolsPanel.setPreferredSize(new java.awt.Dimension(140, 80));

        initMenu();
        
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(workPane, 0, 535, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(toolsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(settingPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(palettePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(toolsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(settingPanel, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(palettePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(workPane, 0, 340, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED))))
        );

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
    			{".&Save", "global.save_image", null},
    			{".Save &As", "global.save_image_as", null},
    			{".-"},
    			{".Export", "global.export", null},
    			{".-"},
    			{".Debug &Color", "global.debug_color", null},
    			
    			{"&Edit", null, null},
    			{".&Undo", "global.undo", null},
    			{".&Redo", "global.redo", null},
    			
    			{"&Window", null, null},
    			{".&Dialogs", null, null},
    			{"..&Layers", "frame.showLayerFrame", null},
    			{"..&Tools", "frame.showToolsFrame", null},
    			{"..-"},
    			{"..Animation &Scheme", "frame.showAnimSchemeFrame", null},
    			{"..Undo &History", "frame.showUndoFrame", null},
    			
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
    	
        if( command.equals("zoom_in")) {
            int zl = workPanel.getZoomLevel();
            if( zl >= 11)
                workPanel.zoom(((zl+1)/4)*4 + 3);   // Arithmetic's a little unintuitive because of zoom_level's off by 1
            else if( zl >= 3)
                workPanel.zoom(((zl+1)/2)*2 + 1);
            else
                workPanel.zoom(zl+1);
        }
        else if( command.equals("zoom_out")) {
            int zl = workPanel.getZoomLevel();
            if( zl > 11)
                workPanel.zoom((zl/4)*4-1);
            else if( zl > 3)
                workPanel.zoom((zl/2)*2-1);
            else
                workPanel.zoom(zl-1);
        }
        else if( command.equals("zoom_in_slow")) {
            workPanel.zoom(workPanel.getZoomLevel()+1);
        }
        else if( command.equals("zoom_out_slow")) {
            workPanel.zoom(workPanel.getZoomLevel()-1);
        }
        else if( command.equals("zoom_0")) {
            workPanel.zoom(0);
        }
        else {
        	MDebug.handleWarning( MDebug.WarningType.REFERENCE, this, "Unknown contextual command: context." + command);
        }
    }
    
    /** Performs the given hotkey command string (should be of "global." focus). */
    private void globalHotkeyCommand( String command) {
        if( command.equals("new_image"))
        	promptNewImage();
        else if( command.equals("debug_color"))
        	promptDebugColor();
        else if( command.equals("newLayerQuick")) {
        	ImageWorkspace workspace = master.getCurrentWorkspace();
        	if( workspace != null) {
        		workspace.setSelectedNode(
        			workspace.addNewLayer( 
        				workspace.getSelectedNode(), 
        				workspace.getWidth(), 
        				workspace.getHeight(), 
        				"newLayer", 
        				new Color(0,0,0,0)));
        		
        		
        	}
        }
        else if( command.equals("open_image"))
			try {
				File f =Dialogs.pickFileOpen();
				
				if( f != null) {
					master.addWorkpace( 
						LoadEngine.loadWorkspace( f), true);
					master.getSettingsManager().setOpennedFile(f);
				}
			} catch (BadSIFFFileException e) {
				MDebug.handleError( ErrorType.FILE, e, "Malformed SIF file.");
			}
        else if( command.equals("save_image")) {
        	ImageWorkspace workspace = master.getCurrentWorkspace();

        	File f=workspace.getFile();

        	if( !workspace.hasChanged() || f == null) {
	        	if( f == null)
	        		f = Dialogs.pickFileSave();
	        	
	        	if( f != null)
					SaveEngine.saveWorkspace( workspace, f);
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
		else if( command.equals("export")) {
			File f = Dialogs.pickFileSave();
			
			if( f != null) {
				RenderSettings settings = new RenderSettings();
				settings.workspace = master.getCurrentWorkspace();
				try {
					ImageIO.write(
							master.getRenderEngine().renderImage(settings),
							"png",
							f);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
        else {
        	MDebug.handleWarning( MDebug.WarningType.REFERENCE, this, "Unknown global command: global." + command);
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
