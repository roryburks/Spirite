package spirite.brains;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.activation.UnsupportedDataTypeException;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.MUtil;
import spirite.MUtil.TransferableImage;
import spirite.brains.RenderEngine.RenderSettings;
import spirite.brains.ToolsetManager.Tool;
import spirite.dialogs.Dialogs;
import spirite.file.LoadEngine;
import spirite.file.SaveEngine;
import spirite.graphics.gl.engine.GLCache;
import spirite.graphics.gl.engine.GLEngine;
import spirite.graphics.gl.engine.GLEngine.MGLException;
import spirite.image_data.GroupTree;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.BuiltImageData;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.image_data.ReferenceManager;
import spirite.image_data.SelectionEngine;
import spirite.image_data.SelectionEngine.BuiltSelection;
import spirite.image_data.SelectionEngine.Selection;
import spirite.image_data.layers.Layer;
import spirite.panel_work.WorkPanel.View;
import spirite.pen.Penner;
import spirite.ui.FrameManager;

/***
 * MasterControl is the top level Model object for all non-UI-related data.
 * For the most part it is little more than a container for all the sub-components
 * which handle the internals of the program, but it also servers two primary 
 * functions itself:
 *	-Interpreting command strings and managing all components which accept command
 *	strings.  
 *		-This includes storing a few CommandExecuters which perform certain
 *		command strings, in particular those in the spaces:
 *			-global.*
 *			-select.*
 *			-draw.*
 *	-Managing the creation and destruction of ImageWorkspaces
 * 
 * Note: Though most UI components will need relatively unobstructed access 
 * 	to MasterControl, giving full access to too many internal components is probably 
 *  indicative of problematic, backwards design.
 * 
 * !!!! NOTE: I have made the decision to allow null workspace selection (particularly
 * 	when none are open). This opens up a lot of scrutiny to be placed on UI components.
 * 
 * @author Rory Burks
 *
 */
public class MasterControl
	implements MImageObserver 
{
	// Components
    private final HotkeyManager hotkeys;
    private final ToolsetManager toolset;
    private final SettingsManager settingsManager;
    private final CacheManager cacheManager;
    private final FrameManager frameManager;
    private final PaletteManager palette;	// Requires SettingsManager
    private final RenderEngine renderEngine;// Require CacheManager
    private final SaveEngine saveEngine;
    private final LoadEngine loadEngine;
    private final Dialogs dialog;
    private GLCache glcache;

    private final List<ImageWorkspace> workspaces = new ArrayList<>();
    private ImageWorkspace currentWorkspace = null;
    private final CommandExecuter executers[];
    

    public MasterControl() {
        settingsManager = new SettingsManager(this);
        hotkeys = new HotkeyManager();
        toolset = new ToolsetManager();
        cacheManager = new CacheManager();
        renderEngine = new RenderEngine( this);	
        palette = new PaletteManager( this);
        loadEngine = new LoadEngine(this);
        saveEngine = new SaveEngine(this);
        dialog = new Dialogs(this);
        frameManager = new FrameManager( this);
//		glcache = new GLCache(this);
		
		settingsManager.setGL( true);
		System.out.println(settingsManager.glMode());
//		initGL();

        // As of now I see no reason to dynamically construct this with a series 
        //	of addCommandExecuter and removeCommandExecuter methods.  I could make
        //	command execution modular that way, but it'd invite forgotten GC links.
        executers = new CommandExecuter[] {
        	new GlobalCommandExecuter(),
        	new RelativeWorkspaceCommandExecuter(),
        	new SelectionCommandExecuter(),
        	toolset,
        	palette,
        	frameManager,
        	frameManager.getRootFrame().getCommandExecuter(),
        	dialog
        };
    }


    // :::: Getters/Setters
    public HotkeyManager getHotekyManager() { return hotkeys; }
    public ToolsetManager getToolsetManager() { return toolset; }
    public PaletteManager getPaletteManager() { return palette; }
    public ImageWorkspace getCurrentWorkspace() { return currentWorkspace; }
    public List<ImageWorkspace> getWorkspaces() { return new ArrayList<>(workspaces); }
    public FrameManager getFrameManager() { return frameManager;}
    public RenderEngine getRenderEngine(){ return renderEngine; }
    public SettingsManager getSettingsManager() { return settingsManager; }
    public CacheManager getCacheManager() { return cacheManager; }
    public SaveEngine getSaveEngine() { return saveEngine; }
    public LoadEngine getLoadEngine() { return loadEngine; }
    public Dialogs getDialogs() { return dialog; }
    public GLCache getGLCache() { return glcache;}
    
    
    // ==============
    // ==== Graphics Engine Management
    
    /** Attempts to initialize OpenGL*/
    boolean initGL() {
    	try {
    		GLEngine.initialize();
    		glcache = new GLCache(this);
    		
    		// TODO: Kind of bad
    		SwingUtilities.invokeLater( new Runnable() {
				@Override public void run() {
		    		frameManager.getWorkPanel().setGL(true);
				}
			});
    	}catch( Exception e) { 
    		MDebug.handleError(ErrorType.ALLOCATION_FAILED, "Could not create OpenGL Context: \n" + e.getMessage());
    		return false;
    	}
    	return true;
    }
    void initAWT() {
		frameManager.getWorkPanel().setGL(false);
    	glcache = null;
    }
    
    // =============
    // ==== Workspace File Open/Save/Load
    
    public void saveWorkspace( ImageWorkspace workspace, File f) {
    	if( workspace == null || f == null) return;
    	saveEngine.saveWorkspace( workspace, f );
		workspace.fileSaved(f);
		saveEngine.removeAutosaved(workspace);
		saveEngine.triggerAutosave(workspace, 5*60, 10);	// Autosave every 5 minutes
    }

    /** 
     * Saves the given workspace to the given standard image file format as a 
     * flat image (made from currently-visible layers).
     * 
     * Supported Image Formats are those supported by the native implementation
     * of ImageIO (PNG, JPG, GIF should be guaranteed to work)
     */
    private void exportWorkspaceToFile( ImageWorkspace workspace, File f) {
    	String ext = f.getName().substring( f.getName().lastIndexOf(".")+1);
    	
    	RenderSettings settings = new RenderSettings(
    			renderEngine.getDefaultRenderTarget(workspace));
    	BufferedImage bi = renderEngine.renderImage(settings);
    	
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
			settingsManager.setImageFilePath(f);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Failed to Export file: " + e.getMessage());
			e.printStackTrace();
		}
    }
    
    public void closeWorkspace( ImageWorkspace workspace) {
    	closeWorkspace(workspace, true);
    }
    public void closeWorkspace( ImageWorkspace workspace, boolean promptSave) {
    	int i = workspaces.indexOf(workspace);
    	
    	if( i == -1) {
    		MDebug.handleError(ErrorType.STRUCTURAL_MINOR, "Tried to remove a workspace that is not being tracked.");
    		return;
    	}
    	
    	if( promptSave && workspace.hasChanged() ) {
    		if( promptSave(workspace) == JOptionPane.CANCEL_OPTION)
    			return;
    	}
    	
    	// Remove the workspace
    	workspace.cleanup();
    	workspace.removeImageObserver(this);
    	workspaces.remove(i);
    	triggerRemoveWorkspace(workspace);
    	
    	if(  workspaces.size() > i) {
    		setCurrentWorkpace( workspaces.get(i));
    	}else if( i > 0){
    		setCurrentWorkpace( workspaces.get(i-1));
    	} else {
    		setCurrentWorkpace(null);
    	}
    	
    }
    
    /** Prompt the User if he wants to save a file, then if he . 
     * 
     * @return 
     * YES_OPTION if saved
     * <br>NO_OPTION if User doesn't want to save
     * <br>CANCEL_OPTION if User cancels
     * */
    public int promptSave( ImageWorkspace workspace ) {
		// Prompt the User to Save the file before closing if it's 
		//	changed and respond accordingly.
    	int ret = JOptionPane.showConfirmDialog(
    			null,
    			"Save " + workspace.getFileName() + " before closing?",
    			"Closing " + workspace.getFileName(),
    			JOptionPane.YES_NO_CANCEL_OPTION,
    			JOptionPane.QUESTION_MESSAGE
    			);
    	
    	if( ret == JOptionPane.CANCEL_OPTION)
    		return ret;
    	
    	if( ret == JOptionPane.YES_OPTION) {
    		File f = workspace.getFile();
    		
    		if( f == null)
    			f = dialog.pickFileSave();
    		
    		if( f != null) {
    			saveEngine.saveWorkspace(workspace, workspace.getFile());
    			saveEngine.removeAutosaved(workspace);
    			settingsManager.setWorkspaceFilePath(f);
    			return ret;
    		}
    		else
    			return JOptionPane.CANCEL_OPTION;
    	}
    	
    	// NO_OPTION
    	return ret;
    }

    
    // ==========
    // ==== Workspace Manipulation
    
    /***
     * Makes the given workspace the currently selected workspace.
     * 
     * Note: if the workspace is not already managed by MasterControl it will add it to
     * 	management, but you should really be using addWorkspace then.
     */
    public void setCurrentWorkpace( ImageWorkspace workspace) {
    	if( currentWorkspace == workspace)
    		return;
    	
    	if( workspace != null && !workspaces.contains(workspace)) {
    		MDebug.handleWarning(WarningType.STRUCTURAL, this, "Tried to assign current workspace to a workspace that MasterControl isn't tracking.");
    		
    		addWorkpace(workspace, false);
    	}
    	
    	ImageWorkspace previous = currentWorkspace;
    	currentWorkspace = workspace;
    	triggerWorkspaceChanged( workspace, previous);
    }
    

    /***
     * Called when you want to add a Workspace that has been algorithmically constructed
     * 	such as with the LoadEngine, rather than making a new one with a default layer.
     * @return 
     */
	public ImageWorkspace addWorkpace(ImageWorkspace workspace, boolean select) {
		workspaces.add(workspace);
		triggerNewWorkspace(workspace);
		
		workspace.addImageObserver(this);
		
		if( select || currentWorkspace == null) {
			setCurrentWorkpace(workspace);
		}
		
		return workspace;
	}
	
	public ImageWorkspace createWorkspaceFromImage( BufferedImage image, boolean select) {
		ImageWorkspace workspace = new ImageWorkspace(this);
		if( image != null)
			workspace.addNewSimpleLayer(null, image, "Base Image");
		workspace.finishBuilding();
		
		this.addWorkpace(workspace, select);
		return workspace;
	}
    

    public void newWorkspace( int width, int height) {newWorkspace(width,height,new Color(0,0,0,0), true);}
    public void newWorkspace( int width, int height, Color color, boolean selectOnCreate) {
    	ImageWorkspace ws = new ImageWorkspace( this);
    	ws.addNewSimpleLayer(null, width, height, "Background", color);
    	
    	workspaces.add( ws);
    	ws.addImageObserver( this);
    	
    	triggerNewWorkspace(ws);
    	if( selectOnCreate || currentWorkspace == null) {
    		setCurrentWorkpace( ws);
    	}
    }
    
    

    // ==========
    // ==== Command Execution
    public void executeCommandString( String command) {
    	String space = (command == null)?"":command.substring(0, command.indexOf("."));
    	String subCommand = command.substring(space.length()+1);
    	
    	boolean executed = false;
    	boolean attempted = false;
    	
    	for( CommandExecuter executer : executers) {
    		if( executer.getCommandDomain().equals(space)) {
    			attempted = true;
    			if(executer.executeCommand(subCommand))
    				executed = true;
    		}
    	}
    	if( !executed) {
    		if( attempted)
    			MDebug.handleWarning( MDebug.WarningType.REFERENCE, this, "Unrecognized command:" + command);
    		else
    			MDebug.handleWarning( MDebug.WarningType.REFERENCE, this, "Unrecognized command domain:" + space);
    	}

    	
    }
    /** Returns a list of all valid Command Strings.  */
    public List<String> getAllValidCommands() {
    	List<String> list = new ArrayList<>();
    	for( CommandExecuter executer : executers) {
    		String domain = executer.getCommandDomain();
    		List<String> commands = executer.getValidCommands();
    		
    		for( String command : commands) {
    			list.add( domain + "." + command);
    		}
    	}
    	
    	return list;
    }
    
    /** Returns a list of all Command String domains. */
    public List<String> getCommandDomains() {
    	List<String> list = new ArrayList<>(executers.length);
    	for( CommandExecuter executer : executers) 
    		list.add(executer.getCommandDomain());
    	return list;
    }
    

    public interface CommandExecuter {
    	public abstract List<String> getValidCommands();
    	public String getCommandDomain();
    	public boolean executeCommand( String command);
    }
    
    /** Command Executer for "global.*" commands.  These are abstract or top-level
     * commands such as "Save", "Undo", "Copy", and "Paste"     */
    class GlobalCommandExecuter implements CommandExecuter {
    	final Map<String, Runnable> commandMap = new HashMap<>();
    	GlobalCommandExecuter() {
    		commandMap.put("save_image", new Runnable() {
				@Override public void run() {
		    		if( currentWorkspace == null)
		    			return;
		    		
		        	File f=currentWorkspace.getFile();

		        	if( currentWorkspace.hasChanged() || f == null) {
			        	if( f == null)
			        		f = dialog.pickFileSave();
			        	
			        	if( f != null) {
			        		saveWorkspace(currentWorkspace, f);
			        		settingsManager.setWorkspaceFilePath(f);
			        	}
		        	}
				}
			});
    		commandMap.put("save_image_as", new Runnable() {
				@Override public void run() {
					File f = dialog.pickFileSave();
					
					if( f != null) {
						saveWorkspace(currentWorkspace, f);
					}
				}
			});
    		commandMap.put("new_image", new Runnable() {
				@Override public void run() {
		    		dialog.promptNewImage();
				}
			});
    		commandMap.put("open_image", new Runnable() {
    			@Override public void run() {
	    			File f =dialog.pickFileOpen();
	    			
	    			if( f != null) {
	    	        	loadEngine.openFile( f);
	    			}
    			}
    		});
    		commandMap.put("export", new Runnable() {@Override public void run() {
				File f = dialog.pickFileExport();
				
				if( f != null) {
					exportWorkspaceToFile( currentWorkspace, f);
				}
			}});
    		commandMap.put("export_as", commandMap.get("export"));
    		commandMap.put("copy", new Runnable() {@Override public void run() {
    			if( currentWorkspace == null) return;
    			Node selected = currentWorkspace.getSelectedNode();
    			
    			if( selected == null) commandMap.get("copyVisible").run();; 
    			
    			if(currentWorkspace.getSelectedNode() != null &&
    				currentWorkspace.getSelectionEngine().getSelection() != null) {
        			// Copies the current selection to the Clipboard

	    	    	BufferedImage img;
    				if( currentWorkspace.getSelectionEngine().isLifted()) {
    					// Copies straight from the lifted data
    					img = currentWorkspace.getSelectionEngine().getLiftedImage();
    				}
    				else {
    					BuiltImageData bid = currentWorkspace.buildActiveData();
    					
    					if( bid == null) {
    		    	    	RenderSettings settings = new RenderSettings(
    		    	    			renderEngine.getNodeRenderTarget(selected));
    		
    		    	    	BufferedImage nodeImg = renderEngine.renderImage(settings);
    						img = currentWorkspace.getSelectionEngine().getBuiltSelection()
    								.liftSelectionFromImage(nodeImg,0,0);
    					}
    					else
	    					img = currentWorkspace.getSelectionEngine().getBuiltSelection()
	    						.liftSelectionFromData(currentWorkspace.buildActiveData());
    				}
    				
	    	    	TransferableImage transfer = new TransferableImage(img);
	    	    	
	    	    	Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
	    	    	c.setContents(transfer, null);
    			}
    			else {
	    			// Copies the current selected node to the Clipboard
	    	    	GroupTree.Node node = currentWorkspace.getSelectedNode();
	
	    	    	RenderSettings settings = new RenderSettings(
	    	    			renderEngine.getNodeRenderTarget(node));
	
	    	    	BufferedImage img = renderEngine.renderImage(settings);
	    	    	TransferableImage transfer = new TransferableImage(img);
	    	    	
	    	    	Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
	    	    	c.setContents(transfer, null);
    			}
    		}});
    		commandMap.put("copyVisible", new Runnable() {@Override public void run() {
    			if( currentWorkspace == null) return;
    			
    			// Copies the current default render to the Clipboard
    			RenderSettings settings = new RenderSettings(
    					renderEngine.getDefaultRenderTarget(currentWorkspace));
//    			settings.workspace = currentWorkspace;
    			
    			// Should be fine to send Clipboard an internal reference since once
    			//	rendered, the RenderEngine's cache should be immutable
    	    	BufferedImage img = renderEngine.renderImage(settings);
    	    	TransferableImage transfer = new TransferableImage(img);
    	    	
    	    	Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
    	    	c.setContents(transfer, null);
    		}});
    		commandMap.put("cut", new Runnable() {@Override public void run() {
    			commandMap.get("copy").run();
    			
    			MasterControl.this.executeCommandString("draw.clearLayer");
    		}});
    		commandMap.put("paste", new Runnable() {@Override public void run() {
    			BufferedImage bi = MUtil.imageFromClipboard();
    			if( bi == null) return;
    			
	    		if( currentWorkspace == null) {
		    		// Create new Workspace from Pasted Data
	    			createWorkspaceFromImage(bi, true);
	    		}
	    		else if( currentWorkspace.buildActiveData() == null){
	    			//	Paste Data as new layer
	    			currentWorkspace.addNewSimpleLayer(currentWorkspace.getSelectedNode(), bi, "Pasted Image");
	    		}
	    		else {
	    			// Paste Data onto Selection Engine (current selected Data)
	    			int ox = 0, oy=0;
	    			
	    			View zoom = frameManager.getZoomerForWorkspace(currentWorkspace);

	    			int min_x = zoom.stiX(0);
	    			int min_y = zoom.stiY(0);
	    			
	    			Node node = currentWorkspace.getSelectedNode();
	    			if( node != null) {
	    				
	    				ox = Math.max(min_x,node.getOffsetX());
	    				oy = Math.max(min_y,node.getOffsetY());
	    			}
	    			
	    			currentWorkspace.getSelectionEngine().imageToSelection(bi, ox, oy);
	    			toolset.setSelectedTool(Tool.BOX_SELECTION);
	    		}
    		}});
    		commandMap.put("pasteAsLayer", new Runnable() {@Override public void run() {
    			BufferedImage bi = MUtil.imageFromClipboard();
    			if( bi == null) return;
    			
	    		if( currentWorkspace == null) {
		    		// Create new Workspace from Pasted Data
	    			createWorkspaceFromImage(bi, true);
	    		}
	    		else {
	    			//	Paste Data as new layer
	    			currentWorkspace.addNewSimpleLayer(currentWorkspace.getSelectedNode(), bi, "Pasted Image");
	    		}
    		}});
    		commandMap.put("toggleGL", new Runnable() {@Override public void run() {
    			settingsManager.setGL( !settingsManager.glMode());
    		}});
    		commandMap.put("toggleGLPanel", new Runnable() {@Override public void run() {
    			frameManager.getWorkPanel().setGL(!frameManager.getWorkPanel().isGLPanel());
    		}});
    	}
    	

		@Override public List<String> getValidCommands() {
			return new ArrayList<>(commandMap.keySet());
		}

		@Override
		public String getCommandDomain() {
			return "global";
		}

		@Override
		public boolean executeCommand(String command) {
			Runnable runnable = commandMap.get(command);
			
			if( runnable != null) {
				runnable.run();
				return true;
			}
			else
				return false;
		}
    }

    /** Command Executre for "draw.*" commands.  These are commands that make
     * direct and immediate changes to the image.     */
    class RelativeWorkspaceCommandExecuter implements CommandExecuter {
    	private final Map<String, Runnable> commandMap = new HashMap<>();
    	
    	// For simplicity's sake, workspaces are stored in the Class
    	//	scope and checked for non-null there before being passed to the
    	//	anonymous Runnable's
    	private ImageWorkspace workspace;
    	RelativeWorkspaceCommandExecuter() {
    		commandMap.put("undo", new Runnable() {@Override public void run() {
    			workspace.getUndoEngine().undo();
    		}});
    		commandMap.put("redo", new Runnable() {@Override public void run() {
				workspace.getUndoEngine().redo();
			}});
    		commandMap.put("shiftRight", new Runnable() {@Override public void run() {
				workspace.shiftData(workspace.getSelectedNode(), 1, 0);
			}});
    		commandMap.put("shiftLeft", new Runnable() {@Override public void run() {
				workspace.shiftData(workspace.getSelectedNode(), -1, 0);
			}});
    		commandMap.put("shiftDown", new Runnable() {@Override public void run() {
				workspace.shiftData(workspace.getSelectedNode(), 0, 1);
    		}});
    		commandMap.put("shiftUp", new Runnable() {@Override public void run() {
				workspace.shiftData(workspace.getSelectedNode(), 0, -1);
			}});
    		commandMap.put("newLayerQuick", new Runnable() {@Override public void run() {
				workspace.addNewSimpleLayer(workspace.getSelectedNode(), 
						workspace.getWidth(), workspace.getHeight(), 
						"New Layer", new Color(0,0,0,0));
    		}});
    		commandMap.put("clearLayer", new Runnable() {@Override public void run() {
				if(!workspace.getSelectionEngine().attemptClearSelection()) {
					// Note: transforms are irrelevant for this action, so 
					//	accessing handle directly is appropriate.
					BuiltImageData image = workspace.buildActiveData();
					if( image != null) 
						workspace.getDrawEngine().clear(image);
				}
			}});
    		commandMap.put("cropSelection", new Runnable() {@Override public void run() {
				Node node = workspace.getSelectedNode();
				SelectionEngine selectionEngine = workspace.getSelectionEngine();
				
				Selection selection = selectionEngine.getSelection();
				if( selection == null) {
					java.awt.Toolkit.getDefaultToolkit().beep();
					return;
				}

				Rectangle rect = new Rectangle(selection.getDimension());
				rect.x = selectionEngine.getOffsetX();
				rect.y = selectionEngine.getOffsetY();
				
				workspace.cropNode(node, rect, false);
			}});
    		commandMap.put("autocroplayer", new Runnable() {@Override public void run() {
				Node node = workspace.getSelectedNode();
				
				if( node instanceof LayerNode) {
					Layer layer = ((LayerNode)node).getLayer();

					try {
						Rectangle rect;
						rect = MUtil.findContentBounds(
								layer.getActiveData().handle.deepAccess(),
								1, 
								false);
						rect.x += node.getOffsetX();
						rect.y += node.getOffsetY();
						workspace.cropNode((LayerNode) node, rect, true);
					} catch (UnsupportedDataTypeException e) {
						e.printStackTrace();
					}
				}
    		}});
    		commandMap.put("layerToImageSize", new Runnable() {@Override public void run() {
				Node node = workspace.getSelectedNode();
				
				if( node != null)
					workspace.cropNode(node, new Rectangle(0,0,workspace.getWidth(), workspace.getHeight()), false);
    		}});
    		commandMap.put("invert", new Runnable() {@Override public void run() {
    			BuiltImageData data= workspace.buildActiveData();
    			
    			if( data != null) {
    				workspace.getDrawEngine().invert(data);
    			}
    		}});
    		commandMap.put("applyTransform", new Runnable() {@Override public void run() {
    			workspace.getSelectionEngine().applyProposedTransform();
    			Penner p = frameManager.getPennerForWorkspace(workspace);
    			if( p != null)
    				p.cleanseState();
    		}});

    		commandMap.put("toggle_reference", new Runnable() {@Override public void run() {
					ReferenceManager rm = workspace.getReferenceManager();
					rm.setEditingReference(!rm.isEditingReference());
			}});
    		commandMap.put("reset_reference", new Runnable() {@Override public void run() {
					ReferenceManager rm = workspace.getReferenceManager();
					rm.resetTransform();
			}});
    		commandMap.put("lift_to_reference", new Runnable() {@Override public void run() {
    			SelectionEngine se = workspace.getSelectionEngine();
    			ReferenceManager rm = workspace.getReferenceManager();
    			
    			BuiltSelection sel = se.getBuiltSelection();
    			BufferedImage bi = se.getLiftedImage();
    			
    			AffineTransform trans = new AffineTransform();
    			trans.translate(sel.offsetX, sel.offsetY);
    			rm.addReference(bi, rm.getCenter(), trans);
    			
    			se.attemptClearSelection();
			}});
    	}

		@Override public List<String> getValidCommands() {
			return new ArrayList<>(commandMap.keySet());
		}

		@Override
		public String getCommandDomain() {
			return "draw";
		}

		@Override
		public boolean executeCommand(String command) {
			Runnable runnable = commandMap.get(command);
			
			if( runnable != null) {
				if( currentWorkspace != null) {
					workspace = currentWorkspace;
					runnable.run();
				}
				return true;
			}
			else
				return false;
		}
    }
    
    /** CommandExecuter for "select.*" commands.  Commands which affect the 
     * selection form.     */
    private class SelectionCommandExecuter implements CommandExecuter {
    	private final Map<String, Runnable> commandMap = new HashMap<>();
    	
    	// For simplicity's sake, stored before executing the command
    	private ImageWorkspace workspace;
    	private SelectionEngine selectionEngine;
    	
    	public SelectionCommandExecuter() {
    		commandMap.put("all", new Runnable() {@Override public void run() {
    			selectionEngine.setSelection( selectionEngine.buildRectSelection(
    					new Rectangle(0,0,workspace.getWidth(), workspace.getHeight())));

    		}});
    		commandMap.put("none", new Runnable() {@Override public void run() {
    			selectionEngine.unselect();
    		}});
    		commandMap.put("invert", new Runnable() {@Override public void run() {
    			BuiltSelection sel = selectionEngine.getBuiltSelection();
    			selectionEngine.setSelection( selectionEngine.invertSelection(sel));
    		}});
		}
    	
		@Override
		public List<String> getValidCommands() {
			return new ArrayList<>(commandMap.keySet());
		}

		@Override
		public String getCommandDomain() {
			return "select";
		}

		@Override
		public boolean executeCommand(String command) {
			Runnable runnable = commandMap.get(command);
			
			if( runnable != null) {
				if( currentWorkspace != null) {
					workspace = currentWorkspace;
					selectionEngine = currentWorkspace.getSelectionEngine();
					runnable.run();
				}
				return true;
			}
			else
				return false;
		}
    	
    }
    
    

    // ===============
    // ==== Observer Interfaces ====
    /***
     * A WorkspaceObserver watches for changes in which workspace is being 
     * actively selected, it doesn't watch for changes inside any Workspace
     * for that you need one of the various Observers in ImageWorkspace
     */
    public static interface MWorkspaceObserver {
        public void currentWorkspaceChanged(  ImageWorkspace selected,  ImageWorkspace previous);
        public void newWorkspace( ImageWorkspace newWorkspace);
        public void removeWorkspace( ImageWorkspace newWorkspace);
    }
    List<WeakReference<MWorkspaceObserver>> workspaceObservers = new ArrayList<>();

    public void addWorkspaceObserver( MWorkspaceObserver obs) {
    	workspaceObservers.add(new WeakReference<MasterControl.MWorkspaceObserver>(obs));
    }
    public void removeWorkspaceObserver( MWorkspaceObserver obs) {
    	Iterator<WeakReference<MWorkspaceObserver>> it = workspaceObservers.iterator();
    	while( it.hasNext()) {
    		MWorkspaceObserver other = it.next().get();
    		if( other == null || other == obs)
    			it.remove();
    	}
    }
    
    private void triggerWorkspaceChanged( ImageWorkspace selected, ImageWorkspace previous) {
    	Iterator<WeakReference<MWorkspaceObserver>> it = workspaceObservers.iterator();
    	while( it.hasNext()) {
    		MWorkspaceObserver other = it.next().get();
    		if( other == null)
    			it.remove();
    		else 
        		other.currentWorkspaceChanged(selected, previous);
    	}
   // 	triggerImageStructureRefresh();
    //	triggerImageRefresh();
    }
    private void triggerNewWorkspace(ImageWorkspace added) {
    	Iterator<WeakReference<MWorkspaceObserver>> it = workspaceObservers.iterator();
    	while( it.hasNext()) {
    		MWorkspaceObserver other = it.next().get();
    		if( other == null)
    			it.remove();
    		else 
        		other.newWorkspace(added);
    	}
    }
    private void triggerRemoveWorkspace(ImageWorkspace removed) {
    	Iterator<WeakReference<MWorkspaceObserver>> it = workspaceObservers.iterator();
    	while( it.hasNext()) {
    		MWorkspaceObserver other = it.next().get();
    		if( other == null)
    			it.remove();
    		else 
        		other.removeWorkspace(removed);
    	}
    }
    
    
    // :::: MCurrentImageObserver
    /***
     * Many components need to watch changes for all ImageWorkspaces.  Instead
     * of adding and removing ImageObservers each time a new ImageWorkspace is
     * added/removed, they can just use a Global Image Observer which will pipe
     * all (existing in Master space) ImageWorkspaces ImageObserver events
     */
    List<WeakReference<MImageObserver>> cimageObservers = new ArrayList<>();

    public void addGlobalImageObserver( MImageObserver obs) { 
    	cimageObservers.add(new WeakReference<MImageObserver>(obs));
    }
    public void removeGlobalImageObserver( MImageObserver obs) { 
    	Iterator<WeakReference<MImageObserver>> it = cimageObservers.iterator();
    	while( it.hasNext()) {
    		MImageObserver other = it.next().get();
    		if( obs == other || other == null)
    			it.remove();
    	}
    }

	// :::: MImageObserver
	@Override
	public void structureChanged(StructureChangeEvent evt) {
    	Iterator<WeakReference<MImageObserver>> it = cimageObservers.iterator();
    	while( it.hasNext()) {
    		MImageObserver other = it.next().get();
    		if( other == null)
    			it.remove();
    		else
    			other.structureChanged(evt);
    	}
	}

	@Override
	public void imageChanged( ImageChangeEvent evt) {
    	Iterator<WeakReference<MImageObserver>> it = cimageObservers.iterator();
    	while( it.hasNext()) {
    		MImageObserver other = it.next().get();
    		if( other == null)
    			it.remove();
    		else
    			other.imageChanged(evt);
    	}
	}


}
