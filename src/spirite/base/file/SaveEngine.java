package spirite.base.file;

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

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.image_data.Animation;
import spirite.base.image_data.GroupTree;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.LogicalImage;
import spirite.base.image_data.animation_data.FixedFrameAnimation;
import spirite.base.image_data.animation_data.FixedFrameAnimation.AnimationLayer;
import spirite.base.image_data.animation_data.FixedFrameAnimation.AnimationLayer.Frame;
import spirite.base.image_data.animation_data.FixedFrameAnimation.Marker;
import spirite.base.image_data.images.DynamicInternalImage;
import spirite.base.image_data.layers.Layer;
import spirite.base.image_data.layers.SimpleLayer;
import spirite.base.image_data.layers.SpriteLayer;
import spirite.base.image_data.layers.SpriteLayer.Part;
import spirite.hybrid.HybridTimer;
import spirite.hybrid.HybridUtil;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;
import spirite.hybrid.MDebug.WarningType;

/***
 * SaveEngine is a static container for methods which saves images to
 * various file formats (but particularly the native SIF format)
 * 
 * TODO: Figure out a stable way to multi-thread backups (one thing is for sure
 * the conversion from BufferedImage to PNG through ImageIO should not
 * be on the UI thread)
 */
public class SaveEngine implements MWorkspaceObserver {
	private final HybridTimer autosaveTimer = new HybridTimer( 10000, new Runnable() {
		@Override
		public void run() {
			long time = System.currentTimeMillis();
			for( AutoSaveWatcher watcher : watchers) {
				if( watcher.workspace.getFile() != null &&
					watcher.interval < (time - watcher.lastTime)/1000 &&
					watcher.undoCount < watcher.workspace.getUndoEngine().getMetronome() - watcher.lastUndoSpot) 
				{
					watcher.lastTime = time;
					saveWorkspace( watcher.workspace, new File(  watcher.workspace.getFile().getAbsolutePath() + "~"), false);
					watcher.lastTime = time;
					watcher.lastUndoSpot= watcher.workspace.getUndoEngine().getMetronome();
				}
			}
			
		}
	});
	
	public SaveEngine( MasterControl master) {
		autosaveTimer.start();
		
		master.addWorkspaceObserver(this);
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
	
	private int lockCount = 0;
	public boolean isLocked() {
		return lockCount != 0;
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
/*		TODO: Reimplement when things are more stable
 * 		File f = workspace.getFile();
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
		lockCount++;
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
			lockCount--;
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
		helper.ra.writeFloat(node.getRender().getAlpha());
		
		// [2] : x offset [2] : y offset
		helper.ra.writeShort( node.getOffsetX());
		helper.ra.writeShort(node.getOffsetY());

		// [1] : bitmask
		int mask = 
				(node.getRender().isVisible() ? SaveLoadUtil.VISIBLE_MASK : 0) | 
				(node.isExpanded() ? SaveLoadUtil.EXPANDED_MASK : 0);

		helper.ra.writeByte( mask);
		
		// [n] : Null-terminated UTF-8 String for Layer name
		helper.ra.write( SaveLoadUtil.strToByteArrayUTF8( node.getName()));

		helper.nodeMap.put( node, helper.nmMet++);
		if( node instanceof GroupTree.GroupNode) {
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
			if( layer instanceof SpriteLayer) {
				SpriteLayer rig = (SpriteLayer)layer;
				List<Part> parts = rig.getParts();
				
				
				// [1] : Node Type ID
				helper.ra.writeByte(SaveLoadUtil.NODE_RIG_LAYER);
				
				// [1] : Number or Parts
				helper.ra.writeByte( parts.size());
				
				// per Part:
				for(Part part : parts) {
					// [n]: null-terminated UTF-8 String
					helper.ra.write(SaveLoadUtil.strToByteArrayUTF8(part.getTypeName()));

					// [4] : TranslationX, [4] TranslationY
					helper.ra.writeFloat(part.getTranslationX());
					helper.ra.writeFloat(part.getTranslationY());
					// [4] : ScaleX, [4] ScaleY
					helper.ra.writeFloat(part.getScaleX());
					helper.ra.writeFloat(part.getScaleY());
					// [4] : Rotation
					helper.ra.writeFloat(part.getRotation());
					
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
		for( LogicalImage part : helper.reservedCache) {
			// (Foreach ImageData)
			
			HybridUtil.savePNG(part.iimg.readOnlyAccess(), bos);

			// [4] : Image ID
			helper.ra.writeInt( part.handleID);
			// [1] : Image Type
			int mask = part.iimg.getType().ordinal();
			helper.ra.writeByte(mask);
			
			if( part.iimg instanceof DynamicInternalImage) {
				// If Dynamic
				// [2] : Dynamic offsetX
				helper.ra.writeShort(part.iimg.getDynamicX());
				// [2] : Dynamic offsetY
				helper.ra.writeShort(part.iimg.getDynamicY());
				
			}
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
		final List<LogicalImage> reservedCache;
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
