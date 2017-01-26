package spirite.file;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.Timer;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.MUtil;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.GroupTree;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.ImageHandle;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.layers.Layer;
import spirite.image_data.layers.SimpleLayer;

/***
 * SaveEngine is a static container for methods which saves images to
 * various file formats (but particularly the native SIF format)
 * 
 * TODO: Implement a thread-locking mechanism preventing RootPanel from
 * 	closing with System.close if there is a Save action going on.
 */
public class SaveEngine implements ActionListener, MWorkspaceObserver {

	private final Timer autosaveTimer = new Timer(10000, this);
	
	public SaveEngine( MasterControl master) {
		autosaveTimer.start();
		
		master.addWorkspaceObserver(this);
	}
	
	
	// Autosave functionality 
	@Override
	public void actionPerformed(ActionEvent evt) {
		long time = System.currentTimeMillis();
		for( AutoSaveWatcher watcher : watchers) {
			if( watcher.workspace.getFile() != null &&
				watcher.interval < (time - watcher.lastTime)/1000 &&
				watcher.undoCount < watcher.workspace.getUndoEngine().getMetronome()) 
			{
				watcher.lastTime = time;
				(new Thread(new Runnable() {
					@Override
					public void run() {
						System.out.println("Saving Backup");
						saveWorkspace( watcher.workspace, new File(  watcher.workspace.getFile().getAbsolutePath() + "~"), false);
						System.out.println("Finished");
					}
				})).start();
				watcher.lastTime = time;
			}
		}
	}
	
	// Behavior might warrant a Map<Workspace,ASW>, but the syntax would
	//	be dirtier and there is no real performanc threat here.
	private final List<AutoSaveWatcher> watchers = new ArrayList<>();
	
	private class AutoSaveWatcher {
		final ImageWorkspace workspace;
		int interval;
		int undoCount;
		long lastTime;
		int lastUndoSpot;
		
		AutoSaveWatcher( ImageWorkspace workspace, int interval, int undoCount) {
			this.workspace = workspace;
			this.interval = interval;
			this.undoCount = undoCount;

			this.lastTime = System.currentTimeMillis();
			this.lastUndoSpot = workspace.getUndoEngine().getMetronome();
		}
	}
	
	/**
	 * If interval is positive then it will auto-save the workspace every 
	 * <code>interval</code> seconds if any changes have been made.  If
	 * <code>undoCount</cod> is positive, it will auto-save every that many
	 * undo's.  If both are set it'll reset only when both conditions are
	 * true.
	 */
	public void triggerAutosave( ImageWorkspace workspace, int interval, int undoCount ) {
		if( interval <= 0 && undoCount <= 0) return;
		
		for( AutoSaveWatcher watcher : watchers) {
			if( watcher.workspace == workspace) {
				watcher.interval = interval;
				watcher.undoCount = undoCount;
				return;
			}
		}
		
		watchers.add(new AutoSaveWatcher(workspace, interval, undoCount));
	}
	public void untriggerAutosave( ImageWorkspace workspace) {
		Iterator<AutoSaveWatcher> it = watchers.iterator();
		while( it.hasNext()) {
			if( it.next().workspace == workspace) {
				it.remove();
			}
		}
	}
	
	public void removeAutosaved( ImageWorkspace workspace) {
		File f = workspace.getFile();
		if( f != null) {
			 f= new File(workspace.getFile().getAbsolutePath() + "~");
			 if( f.exists()) {
				 f.delete();
			 }
		}
	}
	

	/** Attempts to save the workspace to a SIF (native image format) file. */
	public void saveWorkspace( ImageWorkspace workspace, File file) {
		saveWorkspace( workspace, file, true);
	}
	public void saveWorkspace( ImageWorkspace workspace, File file, boolean track) {
		RandomAccessFile ra;
		List<ImageHandle> handles = null;
		
		try {
			// Copies both parts of 
			GroupNode dupeRoot = (GroupNode) workspace.shallowDuplicateNode( workspace.getRootNode());
			handles = workspace.reserveCache();
			
			
			
			if( file.exists()) {
				file.delete();
			}
			file.createNewFile();
			
			ra = new RandomAccessFile(file, "rw");
	
			
			
			// [4] Header
			ra.write( SaveLoadUtil.getHeader());
			// [4]Version
			ra.writeInt(1);
			
			// [2] Width, [2] Height
			ra.writeInt(MUtil.packInt(workspace.getWidth(), workspace.getHeight()));

			// Save Group
			saveGroupTree( dupeRoot, ra);
			
			// Save Image Data
			saveImageData( handles, ra);
			
			ra.close();
			
			if( track)
				workspace.fileSaved(file);
		}catch (UnsupportedEncodingException e) {
			MDebug.handleError(ErrorType.FILE, null, "UTF-8 Format Unsupported (somehow).");
		}catch( IOException e) {
		}
		workspace.relinquishCache(handles);
	}
	
	/** Saves the GRPT chunk containing the Group Tree data */
	private void saveGroupTree( GroupTree.Node root, RandomAccessFile ra) 
		throws UnsupportedEncodingException, IOException
	{
		byte depth = 0;
		ra.write( "GRPT".getBytes("UTF-8"));
		long startPointer = ra.getFilePointer();
		ra.writeInt(0);
		
		_sgt_rec( root, ra, depth);

		long endPointer = ra.getFilePointer();
		if( endPointer - startPointer > Integer.MAX_VALUE )
			MDebug.handleError( ErrorType.OUT_OF_BOUNDS, null, "Image Data Too Big (>2GB).");
		ra.seek(startPointer);
		ra.writeInt( (int)(endPointer - startPointer - 4));
		
		ra.seek(endPointer);
	}
	private void _sgt_rec( GroupTree.Node node, RandomAccessFile ra, byte depth) 
			throws UnsupportedEncodingException, IOException 
	{
		// [1] : Depth of Node in GroupTree
		ra.writeByte( depth);
		
		// [4] : alpha
		ra.writeFloat(node.getAlpha());
		
		// [2] : x offset [2] : y offset
		ra.writeInt( MUtil.packInt(node.getOffsetX(), node.getOffsetY()));

		// [1] : bitmask
		int mask = 
				(node.isVisible() ? SaveLoadUtil.VISIBLE_MASK : 0) + 
				(node.isExpanded() ? SaveLoadUtil.EXPANDED_MASK : 0); 
		ra.writeByte( mask);
		
		if( node instanceof GroupTree.GroupNode) {
			GroupTree.GroupNode gnode = (GroupTree.GroupNode) node;
			// [1] : Node Type ID
			ra.write( SaveLoadUtil.NODE_GROUP);
			
			// [n] : Null-terminated UTF-8 String for Layer name
			ra.write( SaveLoadUtil.strToByteArrayUTF8(gnode.getName()));
			
			// Go through each of the Group Node's children recursively and save them
			for( GroupTree.Node child : node.getChildren()) {
				if( depth == 0xFF)
					MDebug.handleWarning( WarningType.STRUCTURAL, null, "Too many nested groups (255 limit).");
				else {
					_sgt_rec( child, ra, (byte) (depth+1));
				}
			}
		}
		else if( node instanceof GroupTree.LayerNode) {
			Layer layer = ((GroupTree.LayerNode) node).getLayer();
			
			
			if( layer instanceof SimpleLayer) {
				// [1] : Node Type ID
				ra.write( SaveLoadUtil.NODE_SIMPLE_LAYER);
				
				ImageHandle data = ((SimpleLayer) layer).getData();
				// [4] : ID of ImageData linked to this LayerNode
				ra.writeInt( data.getID());
			}
			
			// [n] : Null-terminated UTF-8 String for Layer name
			ra.write( SaveLoadUtil.strToByteArrayUTF8( node.getName()));
		}
		else {
			MDebug.handleWarning(WarningType.STRUCTURAL, null, "Unknown GroupTree Node type on saving.");
		}
	}
	
	/** Saves the IMGD chunk containing the Image data, each in PNG format. */
	private void saveImageData( List<ImageHandle> imageData, RandomAccessFile ra) 
			throws UnsupportedEncodingException, IOException 
	{
		// [4] : "IMGD" tag
		ra.write( "IMGD".getBytes("UTF-8"));
		long filePointer = ra.getFilePointer();
		
		// [4] : Length of ImageData Chunk
		ra.writeInt(0);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		for( ImageHandle part : imageData) {
			// (Foreach ImageData)
			ImageIO.write( part.deepAccess(), "png", bos);

			// [4] : Image ID
			ra.writeInt( part.getID());
			// [4] : Size of Image Data
			ra.writeInt( bos.size());
			// [x] : Image Data
			ra.write(bos.toByteArray());
			bos.reset();
		}
		
		long filePointer2 = ra.getFilePointer();
		ra.seek(filePointer);
		
		if( filePointer2 - filePointer > Integer.MAX_VALUE )
			MDebug.handleError( ErrorType.OUT_OF_BOUNDS, null, "Image Data Too Big (>2GB).");
		ra.writeInt( (int)(filePointer2 - filePointer - 4));
		
		ra.seek(filePointer2);
	}


	// :::: MWorkspaceObserver
	@Override public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {}
	@Override public void newWorkspace(ImageWorkspace newWorkspace) {}
	@Override public void removeWorkspace(ImageWorkspace workspace) {
		untriggerAutosave( workspace);
	}


}
