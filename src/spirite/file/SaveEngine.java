package spirite.file;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.Timer;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.Animation;
import spirite.image_data.GroupTree;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageHandle;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.animation_data.FixedFrameAnimation;
import spirite.image_data.animation_data.FixedFrameAnimation.AnimationLayer;
import spirite.image_data.animation_data.FixedFrameAnimation.AnimationLayer.Frame;
import spirite.image_data.animation_data.FixedFrameAnimation.Marker;
import spirite.image_data.layers.Layer;
import spirite.image_data.layers.RigLayer;
import spirite.image_data.layers.RigLayer.Part;
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
				watcher.undoCount < watcher.workspace.getUndoEngine().getMetronome() - watcher.lastUndoSpot) 
			{
				watcher.lastTime = time;
				(new Thread(new Runnable() {
					@Override
					public void run() {
						MDebug.log("Saving Backup");
						saveWorkspace( watcher.workspace, new File(  watcher.workspace.getFile().getAbsolutePath() + "~"), false);
						MDebug.log("Finished");
					}
				})).start();
				watcher.lastTime = time;
				watcher.lastUndoSpot= watcher.workspace.getUndoEngine().getMetronome();
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
/*		File f = workspace.getFile();
		if( f != null) {
			 f= new File(workspace.getFile().getAbsolutePath() + "~");
			 if( f.exists()) {
				 f.delete();
			 }
		}*/
	}
	

	/** Attempts to save the workspace to a SIF (native image format) file. */
	public void saveWorkspace( ImageWorkspace workspace, File file) {
		saveWorkspace( workspace, file, true);
	}
	public void saveWorkspace( ImageWorkspace workspace, File file, boolean track) {

		SaveHelper helper = new SaveHelper(workspace);

		try {
			if( file.exists()) {
				file.delete();
			}
			file.createNewFile();
			
			helper.ra = new RandomAccessFile(file, "rw");
	
			
			
			// [4] Header
			helper.ra.write( SaveLoadUtil.getHeader());
			// [4]Version
			helper.ra.writeInt(SaveLoadUtil.VERSION);
			
			// [2] Width, [2] Height
			helper.ra.writeShort(workspace.getWidth());
			helper.ra.writeShort(workspace.getHeight());

			// Save Various Chunks
			saveGroupTree( helper);
			saveImageData( helper);
			saveAnimationData( helper);
			
			helper.ra.close();
			
			if( track)
				workspace.fileSaved(file);
		}catch (UnsupportedEncodingException e) {
			MDebug.handleError(ErrorType.FILE, null, "UTF-8 Format Unsupported (somehow).");
		}catch( IOException e) {}
		finally {
			try {
				if( helper.ra != null)
					helper.ra.close();
			} catch (IOException e) {}
		}
		workspace.relinquishCache(helper.reservedCache);
	}
	
	/** Saves the GRPT chunk containing the Group Tree data */
	private void saveGroupTree( SaveHelper helper) 
		throws UnsupportedEncodingException, IOException
	{
		byte depth = 0;
		helper.ra.write( "GRPT".getBytes("UTF-8"));
		long startPointer = helper.ra.getFilePointer();
		helper.ra.writeInt(0);
		
		_sgt_rec( helper, helper.dupeRoot, depth);

		long endPointer = helper.ra.getFilePointer();
		if( endPointer - startPointer > Integer.MAX_VALUE )
			MDebug.handleError( ErrorType.OUT_OF_BOUNDS, null, "Image Data Too Big (>2GB).");
		helper.ra.seek(startPointer);
		helper.ra.writeInt( (int)(endPointer - startPointer - 4));
		
		helper.ra.seek(endPointer);
	}
	// Recursive should be fine since there's a depth limit of 255
	private void _sgt_rec( SaveHelper helper, GroupTree.Node node, byte depth) 
			throws UnsupportedEncodingException, IOException 
	{
		// [1] : Depth of Node in GroupTree
		helper.ra.writeByte( depth);
		
		// [4] : alpha
		helper.ra.writeFloat(node.getAlpha());
		
		// [2] : x offset [2] : y offset
		helper.ra.writeShort( node.getOffsetX());
		helper.ra.writeShort(node.getOffsetY());

		// [1] : bitmask
		int mask = 
				(node.isVisible() ? SaveLoadUtil.VISIBLE_MASK : 0) | 
				(node.isExpanded() ? SaveLoadUtil.EXPANDED_MASK : 0);

		helper.ra.writeByte( mask);
		
		// [n] : Null-terminated UTF-8 String for Layer name
		helper.ra.write( SaveLoadUtil.strToByteArrayUTF8( node.getName()));

		helper.nodeMap.put( node, helper.nmMet++);
		if( node instanceof GroupTree.GroupNode) {
			GroupTree.GroupNode gnode = (GroupTree.GroupNode) node;
			// [1] : Node Type ID
			helper.ra.write( SaveLoadUtil.NODE_GROUP);
			
			// Go through each of the Group Node's children recursively and save them
			for( GroupTree.Node child : node.getChildren()) {
				if( depth == 0xFF)
					MDebug.handleWarning( WarningType.STRUCTURAL, null, "Too many nested groups (255 limit).");
				else {
					_sgt_rec( helper, child, (byte) (depth+1));
				}
			}
		}
		else if( node instanceof GroupTree.LayerNode) {
			Layer layer = ((GroupTree.LayerNode) node).getLayer();
			
			
			if( layer instanceof SimpleLayer) {
				// [1] : Node Type ID
				helper.ra.write( SaveLoadUtil.NODE_SIMPLE_LAYER);
				
				ImageHandle data = ((SimpleLayer) layer).getData();
				// [4] : ID of ImageData linked to this LayerNode
				helper.ra.writeInt( data.getID());
			}
			if( layer instanceof RigLayer) {
				RigLayer rig = (RigLayer)layer;
				List<Part> parts = rig.getParts();
				
				
				// [1] : Node Type ID
				helper.ra.writeByte(SaveLoadUtil.NODE_RIG_LAYER);
				
				// [1] : Number or Parts
				helper.ra.writeByte( parts.size());
				
				// per Part:
				for(Part part : parts) {
					// [n]: null-terminated UTF-8 String
					helper.ra.write(SaveLoadUtil.strToByteArrayUTF8(part.getTypeName()));
					
					// [2] : OffsetX, [2] :OffsetY
					helper.ra.writeShort(part.getOffsetX());
					helper.ra.writeShort(part.getOffsetY());
					
					// [4] : Depth
					helper.ra.writeInt( part.getDepth());
					
					// [4] ImageHandle ID
					helper.ra.writeInt(part.getImageHandle().getID());
				}
			}
		}
		else {
			MDebug.handleWarning(WarningType.STRUCTURAL, null, "Unknown GroupTree Node type on saving.");
		}
	}
	
	/** Saves the IMGD chunk containing the Image data, each in PNG format. */
	private void saveImageData( SaveHelper helper) 
			throws UnsupportedEncodingException, IOException 
	{
		// [4] : "IMGD" tag
		helper.ra.write( "IMGD".getBytes("UTF-8"));
		long filePointer = helper.ra.getFilePointer();
		
		// [4] : Length of ImageData Chunk
		helper.ra.writeInt(0);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		for( ImageHandle part : helper.reservedCache) {
			// (Foreach ImageData)
			ImageIO.write( part.deepAccess(), "png", bos);

			// [4] : Image ID
			helper.ra.writeInt( part.getID());
			// [4] : Size of Image Data
			helper.ra.writeInt( bos.size());
			// [x] : Image Data
			helper.ra.write(bos.toByteArray());
			bos.reset();
		}
		
		long filePointer2 = helper.ra.getFilePointer();
		helper.ra.seek(filePointer);
		
		if( filePointer2 - filePointer > Integer.MAX_VALUE )
			MDebug.handleError( ErrorType.OUT_OF_BOUNDS, null, "Image Data Too Big (>2GB).");
		helper.ra.writeInt( (int)(filePointer2 - filePointer-4));
		
		helper.ra.seek(filePointer2);
	}
	
	/** Saves the ANIM chunk containing the Animation structures */
	private void saveAnimationData( SaveHelper helper)
			throws UnsupportedEncodingException, IOException 
	{
		List<Animation> animations = helper.workspace.getAnimationManager().getAnimations();
		
		// [4] : "ANIM" tag
		helper.ra.write( "ANIM".getBytes("UTF-8"));
		
		long start = helper.ra.getFilePointer();
		// [4] : ChunkLength (placeholder for now)
		helper.ra.writeInt(0);
		
		for( Animation animation : animations) {
			// [n] : UTF8: Animation Name
			helper.ra.write(SaveLoadUtil.strToByteArrayUTF8(animation.getName()));
			
			if( animation instanceof FixedFrameAnimation) {
				// [1] : ID
				helper.ra.writeByte(SaveLoadUtil.ANIM_FIXED_FRAME);
				
				// [2] : Number of Layers
				List<AnimationLayer>layers = ((FixedFrameAnimation) animation).getLayers();
				helper.ra.writeShort( layers.size());
				
				for( AnimationLayer layer : layers) {
					// [4] : Group Node Bound to
					helper.ra.writeInt(helper.nodeMap.get(layer.getGroupLink()));
					
					// [2] : Number of Frames
					List<Frame> frames = layer.getFrames();
					helper.ra.writeShort(frames.size());
					
					for( Frame frame : frames) {
						// [1] : Marker
						helper.ra.writeByte(frame.getMarker().ordinal());
						
						// [2] : Length
						helper.ra.writeShort(frame.getLength());
						
						if( frame.getMarker() == Marker.FRAME) {
							// [4] : LayerID (corresponding to order it appears in the file)
							helper.ra.writeInt( helper.nodeMap.get(frame.getLayerNode()));
						}
					}
				}
				
			}
			else {
				helper.ra.writeByte(SaveLoadUtil.UNKNOWN);
			}
		}
		
		
		long end = helper.ra.getFilePointer();
		helper.ra.seek(start);
		if( end - start> Integer.MAX_VALUE )
			MDebug.handleError( ErrorType.OUT_OF_BOUNDS, null, "Image Data Too Big (>2GB).");
		helper.ra.writeInt( (int)(end - start-4));
	}
	
	private class SaveHelper {
		final Map<Node, Integer> nodeMap = new HashMap<>();
		int nmMet = 0;
		final List<ImageHandle> reservedCache;
		final GroupNode dupeRoot;
		final ImageWorkspace workspace;
		RandomAccessFile ra;
		
		
		SaveHelper( ImageWorkspace workspace) {
			this.workspace = workspace;
			reservedCache = workspace.reserveCache();
			dupeRoot =  workspace.getRootNode();
		}
	}


	// :::: MWorkspaceObserver
	@Override public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {}
	@Override public void newWorkspace(ImageWorkspace newWorkspace) {}
	@Override public void removeWorkspace(ImageWorkspace workspace) {
		untriggerAutosave( workspace);
	}


}
