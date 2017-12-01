package spirite.base.brains;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import com.jogamp.opengl.GL2;

import spirite.base.brains.commands.CommandExecuter;
import spirite.base.brains.commands.GlobalCommandExecuter;
import spirite.base.brains.commands.NodeContextCommand;
import spirite.base.brains.commands.RelativeWorkspaceCommandExecuter;
import spirite.base.brains.commands.SelectionCommandExecuter;
import spirite.base.file.LoadEngine;
import spirite.base.file.SaveEngine;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.gl.GLEngine;
import spirite.base.graphics.renderer.RenderEngine;
import spirite.base.graphics.renderer.RenderEngine.RenderSettings;
import spirite.base.image_data.AnimationManager.MAnimationStateObserver;
import spirite.base.image_data.AnimationManager.MAnimationStructureObserver;
import spirite.base.image_data.AnimationView.MAnimationViewObserver;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.ImageWorkspace.MFlashObserver;
import spirite.base.image_data.ImageWorkspace.MImageObserver;
import spirite.base.image_data.ImageWorkspace.MNodeSelectionObserver;
import spirite.base.image_data.ImageWorkspace.MWorkspaceFileObserver;
import spirite.base.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.base.image_data.mediums.IMedium.InternalImageTypes;
import spirite.base.util.ObserverHandler;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.HybridUtil;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;
import spirite.hybrid.MDebug.WarningType;
import spirite.pc.jogl.JOGLCore;
import spirite.pc.ui.ContextMenus;
import spirite.pc.ui.dialogs.Dialogs;
import spirite.pc.ui.omni.FrameManager;

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
{
	// Components
    private final HotkeyManager hotkeys;
    private final ToolsetManager toolset;
    private final SettingsManager settingsManager;
    private final FrameManager frameManager;
    private final PaletteManager palette;	// Requires SettingsManager
    private final RenderEngine renderEngine;// Require CacheManager
    private final SaveEngine saveEngine;
    private final LoadEngine loadEngine;
    private final Dialogs dialog;
    private final ContextMenus contextMenus;

    private final List<ImageWorkspace> workspaces = new ArrayList<>();
    private ImageWorkspace currentWorkspace = null;
    private final CommandExecuter executers[];
    

    public MasterControl() {
        settingsManager = new SettingsManager(this);
        hotkeys = new HotkeyManager();
        toolset = new ToolsetManager(this);
        renderEngine = new RenderEngine( this);	
        palette = new PaletteManager( this);
        loadEngine = new LoadEngine(this);
        saveEngine = new SaveEngine(this);
        dialog = new Dialogs(this);
        frameManager = new FrameManager( this);
        contextMenus = new ContextMenus(this);
		
		settingsManager.setGL( true);

        // As of now I see no reason to dynamically construct this with a series 
        //	of addCommandExecuter and removeCommandExecuter methods.  I could make
        //	command execution modular that way, but it'd invite forgotten GC links.
        executers = new CommandExecuter[] {
        	new GlobalCommandExecuter(this),
        	new RelativeWorkspaceCommandExecuter(this),
        	new SelectionCommandExecuter(this),
        	new NodeContextCommand(this),
        	toolset,
        	palette,
        	frameManager,
        	frameManager.getRootFrame().getCommandExecuter(),
        	dialog
        };
        
        // One of the few usages of invokeLater that seems appropriate
        HybridHelper.queueToRun( () -> {
	        createNewWorkspace(640,480,0x00000000, true);
	        getCurrentWorkspace().finishBuilding();
        });
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
    public SaveEngine getSaveEngine() { return saveEngine; }
    public LoadEngine getLoadEngine() { return loadEngine; }
    public Dialogs getDialogs() { return dialog; }
    public ContextMenus getContextMenus() { return contextMenus; }
    
    
    // ==============
    // ==== Graphics Engine Management
    
    /** Attempts to initialize OpenGL*/
    boolean initGL() {
    	try {
    		GLEngine engine = GLEngine.getInstance();
    		JOGLCore.init( (GL2 gl) ->{engine.init(gl);});
    		
    		// NOTE: Kind of bad, but probably necessary.  Might require some locks to prevent bad things happening
    		HybridHelper.queueToRun( () -> {frameManager.getWorkPanel().setGL(true);});
    	}catch( Exception e) { 
    		MDebug.handleError(ErrorType.ALLOCATION_FAILED, "Could not create OpenGL Context: \n" + e.getMessage());
    		return false;
    	}
    	return true;
    }
    void initAWT() {
		frameManager.getWorkPanel().setGL(false);
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
    public void exportWorkspaceToFile( ImageWorkspace workspace, File f) {
    	String ext = f.getName().substring( f.getName().lastIndexOf(".")+1);
    	
    	RenderSettings settings = new RenderSettings(
    			renderEngine.getDefaultRenderTarget(workspace));
    	RawImage img = renderEngine.renderImage(settings);
    	
    	
    	if( ext.equals("jpg") || ext.equals("jpeg")) {
    		// Remove Alpha Layer of JPG so that encoding works correctly
    		RawImage img2 = img;
    		img = HybridHelper.createImage( img2.getWidth(), img2.getHeight());
    		
    		GraphicsContext gc = img.getGraphics();
    		gc.drawImage( img2, 0, 0);
    	}
    	
    	try {
    		HybridUtil.saveEXT( img, ext, f);
			settingsManager.setImageFilePath(f);
		} catch (IOException e) {
			HybridHelper.showMessage( "", "Failed to Export file: " + e.getMessage());
			e.printStackTrace();
		}
    }


    MImageObserver imageObserver = new MImageObserver() {
		@Override
		public void imageChanged(ImageChangeEvent evt) {

			cimageObs.trigger((MImageObserver obs) -> {obs.imageChanged(evt);});
		}

		@Override
		public void structureChanged(StructureChangeEvent evt) {

			cimageObs.trigger((MImageObserver obs) -> {obs.structureChanged(evt);});
		}
	};

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
    	workspace.removeImageObserver(imageObserver);
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
    		
    		addCreatedWorkpace(workspace, false);
    	}
    	
    	ImageWorkspace previous = currentWorkspace;
    	currentWorkspace = workspace;
    	triggerWorkspaceChanged( workspace, previous);
    }
    

    /***
     * Called when you want to add a Workspace that has been algorithmically constructed
     * 	such as with the LoadEngine, rather than making a new one with a default layer.
     */
	public ImageWorkspace addCreatedWorkpace(ImageWorkspace workspace, boolean select) {
		workspaces.add(workspace);
		triggerNewWorkspace(workspace);
		
		workspace.addImageObserver(imageObserver);
		
		if( select || currentWorkspace == null) {
			setCurrentWorkpace(workspace);
		}
		
		return workspace;
	}
	
	public ImageWorkspace createWorkspaceFromImage( RawImage image, boolean select) {
		ImageWorkspace workspace = new ImageWorkspace(this);
		if( image != null)
			workspace.addNewSimpleLayer(null, image, "Base Image", InternalImageTypes.NORMAL);
		workspace.finishBuilding();
		
		this.addCreatedWorkpace(workspace, select);
		return workspace;
	}
    

    public void createNewWorkspace( int width, int height) {createNewWorkspace(width,height, 0x00000000, true);}
    public void createNewWorkspace( int width, int height, int color, boolean selectOnCreate) {
    	ImageWorkspace ws = new ImageWorkspace( this);
    	ws.addNewSimpleLayer(null, width, height, "Background", color, InternalImageTypes.NORMAL);
    	
    	workspaces.add( ws);
    	ws.addImageObserver( imageObserver);
    	
    	triggerNewWorkspace(ws);
    	if( selectOnCreate || currentWorkspace == null) {
    		setCurrentWorkpace( ws);
    	}
    }
    
    

    // ==========
    // ==== Command Execution
    public void executeCommandString( String command) {
    	executeCommandString(command, null);
    }
    public void executeCommandString( String command, Object extra) {
    	String space = (command == null)?"":command.substring(0, command.indexOf("."));
    	String subCommand = command.substring(space.length()+1);
    	
    	boolean executed = false;
    	boolean attempted = false;
    	
    	for( CommandExecuter executer : executers) {
    		if( executer.getCommandDomain().equals(space)) {
    			attempted = true;
    			if(executer.executeCommand(subCommand, extra))
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
    

    // ===============
    // ==== Observer Interfaces
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
    private final ObserverHandler<MWorkspaceObserver> workspaceObs = new ObserverHandler<>();
    public void addWorkspaceObserver( MWorkspaceObserver obs) { workspaceObs.addObserver(obs);}
    public void removeWorkspaceObserver( MWorkspaceObserver obs) {workspaceObs.removeObserver(obs);}
    
    private void triggerWorkspaceChanged( ImageWorkspace selected, ImageWorkspace previous) {
    	_triggerWSChangedForTracking( selected, previous);
    	workspaceObs.trigger((MWorkspaceObserver obs)->{obs.currentWorkspaceChanged(selected, previous);});
   // 	triggerImageStructureRefresh();
    //	triggerImageRefresh();
    }
    private void triggerNewWorkspace(ImageWorkspace added) {
    	workspaceObs.trigger((MWorkspaceObserver obs)->{obs.newWorkspace(added);});
    }
    private void triggerRemoveWorkspace(ImageWorkspace removed) {
    	workspaceObs.trigger((MWorkspaceObserver obs)->{obs.removeWorkspace(removed);});
    }
    
    
    // :::: MCurrentImageObserver
    /***
     * Many components need to watch changes for all ImageWorkspaces.  Instead
     * of adding and removing ImageObservers each time a new ImageWorkspace is
     * added/removed, they can just use a Global Image Observer which will pipe
     * all (existing in Master space) ImageWorkspaces ImageObserver events
     */
    private final ObserverHandler<MImageObserver> cimageObs = new ObserverHandler<>();
    public void addGlobalImageObserver( MImageObserver obs) { cimageObs.addObserver(obs);}
    public void removeGlobalImageObserver( MImageObserver obs) { cimageObs.removeObserver(obs);}
	
	// :::: Tracking Map Observer:
	private class TrackingObserver<T> {
		private final WeakReference<T> baseObserver;
		private final FastTrack<T> ftt;
		private TrackingObserver( T obs, FastTrack<T> ftt) {
			this.baseObserver = new WeakReference<>(obs);
			this.ftt = ftt;
		}
		private void rem(ImageWorkspace ws) {ftt.onRemove.Do(ws, baseObserver.get()); }
		private void add(ImageWorkspace ws) {ftt.onAdd.Do(ws, baseObserver.get()); }
	}
	private interface DoOnWS<T> { void Do(ImageWorkspace ws, T obs);}
	private static class FastTrack<T> {
		DoOnWS<T> onAdd;
		DoOnWS<T> onRemove;
		FastTrack( DoOnWS<T> add, DoOnWS<T> rem) {this.onAdd = add; this.onRemove = rem;}
	}
	
	private final List<TrackingObserver<?>> trackingObservers = new ArrayList<>();
	
	/**
	 * Since so many UI components need to update their observers every time that the workspace
	 * is changed, TrackingObservers exist to streamline the process.  Instead of adding a 
	 * workspace observer and removing/adding observers as they're changed, a Tracking Observer
	 * does the work for you.
	 */
	public <T> void addTrackingObserver(Class<T> tclass, T observer) {
		// Should by typesafe everywhere except here, and here it's typesafe assuming
		//	trackMap is correctly constructed
		@SuppressWarnings("unchecked")
		FastTrack<T> ftt = (FastTrack<T>)trackMap.get(tclass);
		
		if( ftt == null)
			MDebug.handleError(ErrorType.STRUCTURAL_MAJOR, null, "Failed to find the requested observer type.");
		
		TrackingObserver<T> newObs = new TrackingObserver<T>(observer, ftt);
		trackingObservers.add(newObs);
		
		if( currentWorkspace != null) {
			ftt.onAdd.Do(currentWorkspace, observer);
		}
	}
	public void removeTrackingObserver( Object observer) {
		Iterator<TrackingObserver<?>> it = trackingObservers.iterator();
		
		while( it.hasNext()) {
			TrackingObserver<?> tobs = it.next();
			if( tobs.baseObserver.get() == null || tobs.baseObserver.get() == observer)
				it.remove();
		}
	}
	
	private void _triggerWSChangedForTracking(ImageWorkspace newWorkspace, ImageWorkspace oldWorkspace) {
		Iterator<TrackingObserver<?>> it = trackingObservers.iterator();
		
		while( it.hasNext()) {
			TrackingObserver<?> tobs = it.next();
			if( tobs.baseObserver.get() == null)
				it.remove();
			else {
				if( oldWorkspace != null)
					tobs.rem(oldWorkspace);
				if( newWorkspace != null)
					tobs.add(newWorkspace);
			}
		}
	}
	
	// Ugly, but the alternative (somehow giving Master a moving track of all the ObserverHandlers)
	//	is far far too much a convoluted pain in the ass.
	private final static Map<Class<?>,FastTrack<?>> trackMap = new HashMap<>();
	static {
		// ImageWorkapace
		trackMap.put(MImageObserver.class, new FastTrack<MImageObserver>( 
				(ImageWorkspace ws, MImageObserver obs)->ws.addImageObserver(obs),
				(ImageWorkspace ws, MImageObserver obs)->ws.removeImageObserver(obs)));
		trackMap.put(MNodeSelectionObserver.class, new FastTrack<MNodeSelectionObserver>( 
				(ImageWorkspace ws, MNodeSelectionObserver obs)->ws.addSelectionObserver(obs),
				(ImageWorkspace ws, MNodeSelectionObserver obs)->ws.removeSelectionObserver(obs)));
		trackMap.put(MWorkspaceFileObserver.class, new FastTrack<MWorkspaceFileObserver>( 
				(ImageWorkspace ws, MWorkspaceFileObserver obs)->ws.addWorkspaceFileObserve(obs),
				(ImageWorkspace ws, MWorkspaceFileObserver obs)->ws.removeWorkspaceFileObserve(obs)));
		trackMap.put(MFlashObserver.class, new FastTrack<MFlashObserver>( 
				(ImageWorkspace ws, MFlashObserver obs)->ws.addFlashObserve(obs),
				(ImageWorkspace ws, MFlashObserver obs)->ws.removeFlashObserve(obs)));
		
		// AnimationManager
		trackMap.put(MAnimationStructureObserver.class, new FastTrack<MAnimationStructureObserver>( 
				(ImageWorkspace ws, MAnimationStructureObserver obs)->ws.getAnimationManager().addAnimationStructureObserver(obs),
				(ImageWorkspace ws, MAnimationStructureObserver obs)->ws.getAnimationManager().removeAnimationStructureObserver(obs)));
		trackMap.put(MAnimationStateObserver.class, new FastTrack<MAnimationStateObserver>( 
				(ImageWorkspace ws, MAnimationStateObserver obs)->ws.getAnimationManager().addAnimationStateObserver(obs),
				(ImageWorkspace ws, MAnimationStateObserver obs)->ws.getAnimationManager().removeAnimationStateObserver(obs)));
		
		// AnimationView
		trackMap.put(MAnimationViewObserver.class, new FastTrack<MAnimationViewObserver>(
				(ImageWorkspace ws, MAnimationViewObserver obs)->ws.getAnimationManager().getView().addAnimationViewObserver(obs),
				(ImageWorkspace ws, MAnimationViewObserver obs)->ws.getAnimationManager().getView().removeAnimationViewObserver(obs)));
	}
}