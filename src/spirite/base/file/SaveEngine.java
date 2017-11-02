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

import javax.swing.JOptionPane;

import javafx.util.Pair;
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
import spirite.base.image_data.animation_data.RigAnimation;
import spirite.base.image_data.animation_data.RigAnimation.PartKeyFrame;
import spirite.base.image_data.animation_data.RigAnimation.RigAnimLayer;
import spirite.base.image_data.images.PrismaticMedium;
import spirite.base.image_data.images.maglev.MaglevMedium;
import spirite.base.image_data.images.maglev.MaglevMedium.MagLevFill;
import spirite.base.image_data.images.maglev.MaglevMedium.MagLevStroke;
import spirite.base.image_data.images.maglev.MaglevMedium.MagLevThing;
import spirite.base.image_data.layers.Layer;
import spirite.base.image_data.layers.ReferenceLayer;
import spirite.base.image_data.layers.SimpleLayer;
import spirite.base.image_data.layers.SpriteLayer;
import spirite.base.image_data.layers.SpriteLayer.Part;
import spirite.base.pen.PenTraits.PenState;
import spirite.hybrid.HybridTimer;
import spirite.hybrid.HybridUtil;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;
import spirite.hybrid.MDebug.WarningType;

/***
 * SaveEngine is a static container for methods which saves images to
 * various file formats (but particularly the native SIF format)
 */
public class SaveEngine implements MWorkspaceObserver {
	private final MasterControl master;
	boolean MULTITHREADED = false;
	
	private final HybridTimer autosaveTimer;
	
	public SaveEngine( MasterControl master) {
		this.master = master;
		
		final Runnable tick = () -> {
			master.getFrameManager().getWorkPanel().setMessage("Saving File...");
			long time = System.currentTimeMillis();
			for( AutoSaveWatcher watcher : watchers) {
				if( watcher.workspace.getFile() != null &&
					watcher.interval < (time - watcher.lastTime)/1000 &&
					watcher.undoCount < watcher.workspace.getUndoEngine().getMetronome() - watcher.lastUndoSpot) 
				{
					watcher.lastTime = time;
					if(!saveWorkspace( watcher.workspace, new File(  watcher.workspace.getFile().getAbsolutePath() + "~"), false))
						JOptionPane.showMessageDialog(null, "Failed to autosave");
					watcher.lastTime = time;
					watcher.lastUndoSpot= watcher.workspace.getUndoEngine().getMetronome();
				}
			}
			master.getFrameManager().getWorkPanel().setMessage("");
		};
		
		autosaveTimer = new HybridTimer( 10000, () -> {
			if( MULTITHREADED) {
				(new Thread(tick)).run();
			}
			else
				tick.run();
		});
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
		if( master.getSettingsManager().getBoolSetting("removeAutoSaves", false)) {
			File f = workspace.getFile();
			if( f != null) {
				 f= new File(workspace.getFile().getAbsolutePath() + "~");
				 if( f.exists()) {
					 f.delete();
				 }
			}
		}
	}
	

	/** Attempts to save the workspace to a SIF (native image format) file. */
	public boolean saveWorkspace( ImageWorkspace workspace, File file) {
		return saveWorkspace( workspace, file, true);
	}
	public boolean saveWorkspace( ImageWorkspace workspace, File file, boolean track) {
		lockCount++;
		SaveHelper helper = new SaveHelper(workspace);
		workspace.getAnimationManager().purge();
		boolean good=  true;

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
			good = false;
		}catch( IOException e) {good = false;}
		finally {
			try {
				if( helper.ra != null)
					helper.ra.close();
			} catch (IOException e) {}
			lockCount--;
		}
		workspace.relinquishCache(helper.reservedCache);
		return good;
	}
	
	/** Saves the GRPT chunk containing the Group Tree data */
	private void saveGroupTree( SaveHelper helper) 
		throws UnsupportedEncodingException, IOException
	{
		byte depth = 0;
		helper.ra.write( "GRPT".getBytes("UTF-8"));
		long startPointer = helper.ra.getFilePointer();
		helper.ra.writeInt(0);
		
		_sgt_buildReferences_rec( helper, helper.dupeRoot, depth);
		_sgt_rec( helper, helper.dupeRoot, depth);

		long endPointer = helper.ra.getFilePointer();
		if( endPointer - startPointer > Integer.MAX_VALUE )
			MDebug.handleError( ErrorType.OUT_OF_BOUNDS, null, "Image Data Too Big (>2GB).");
		helper.ra.seek(startPointer);
		helper.ra.writeInt( (int)(endPointer - startPointer - 4));
		
		helper.ra.seek(endPointer);
	}
	private void _sgt_buildReferences_rec( SaveHelper helper, GroupTree.Node node, byte depth) 
			throws UnsupportedEncodingException, IOException 
	{
		helper.nodeMap.put( node, helper.nmMet++);
		if( node instanceof GroupNode) {
			for( GroupTree.Node child : node.getChildren())
				_sgt_buildReferences_rec( helper, child, (byte) (depth+1));
		}
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
			if( layer instanceof ReferenceLayer) {
				// [1] : Node Type ID
				helper.ra.writeByte(SaveLoadUtil.NODE_REFERENCE_LAYER);
				// [4] : NodeID of referenced node
				helper.ra.writeInt(helper.nodeMap.get(((ReferenceLayer)layer).getUnderlying()));
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
			part.Float();			

			// [4] : Image ID
			helper.ra.writeInt( part.handleID);
			// [1] : Image Type
			int mask = part.iimg.getType().ordinal();
			helper.ra.writeByte(mask);
			
			switch( part.iimg.getType()) {
			case NORMAL:
				HybridUtil.savePNG(part.iimg.readOnlyAccess(), bos);
				// [4] : Size of Image Data
				helper.ra.writeInt( bos.size());
				// [x] : Image Data
				helper.ra.write(bos.toByteArray());
				bos.reset();
				break;
			case DYNAMIC: {
				// [2] : Dynamic offsetX
				helper.ra.writeShort(part.iimg.getDynamicX());
				// [2] : Dynamic offsetY
				helper.ra.writeShort(part.iimg.getDynamicY());
				
				HybridUtil.savePNG(   part.iimg.readOnlyAccess(), bos);
				// [4] : Size of Image Data
				helper.ra.writeInt( bos.size());
				// [x] : Image Data
				helper.ra.write(bos.toByteArray());
				bos.reset();
				break;}
			case PRISMATIC:{
				PrismaticMedium pii = (PrismaticMedium)part.iimg;
				List<PrismaticMedium.LImg> list = pii.getColorLayers();
				
				// [2] : Number of Color Layers
				helper.ra.writeShort(list.size());

				for( PrismaticMedium.LImg limg : list) {
					// [4] : color
					helper.ra.writeInt(limg.color);
					// [2, 2] : Dynamic offset X,Y
					helper.ra.writeShort( limg.ox);
					helper.ra.writeShort( limg.oy);

					HybridUtil.savePNG(limg.img, bos);
					// [4] : Size of Image Data
					helper.ra.writeInt( bos.size());
					// [x] : Image Data
					helper.ra.write(bos.toByteArray());
					bos.reset();
				}
				break;}
			case MAGLEV: {
				MaglevMedium mimg = (MaglevMedium)part.iimg;
				
				List<MagLevThing> things = mimg.getThings();
				//	[2] : Number of things
				helper.ra.writeShort(things.size());	
				
				for( MagLevThing thing : things) {
					if( thing instanceof MagLevStroke) {
						MagLevStroke stroke = (MagLevStroke)thing;
						
						helper.ra.writeByte(0);		//	[1] : thing type
						
						helper.ra.writeInt( stroke.params.getColor());			// [4] : Color
						helper.ra.writeByte( stroke.params.getMethod().fileId);	// [1] : Method Type
						helper.ra.writeFloat(stroke.params.getWidth());			// [4] : Width
						
						helper.ra.writeShort( stroke.states.length);	// [2] : Number of Vertices
						
						for( PenState ps : stroke.states) {
							helper.ra.writeFloat( ps.x);		// [4] : x
							helper.ra.writeFloat( ps.y);		// [4] : y
							helper.ra.writeFloat( ps.pressure);	// [4] : pressure
						}
						
					}
					else if( thing instanceof MagLevFill) {
						MagLevFill fill = (MagLevFill)thing;
						helper.ra.writeByte(1);		// [1] : thing type

						helper.ra.writeInt( fill.getColor());			// [4] : Color
						
						helper.ra.writeShort(fill.segments.size());	// [2] : number of segments

						for( MaglevMedium.MagLevFill.StrokeSegment seg : fill.segments) {
							helper.ra.writeShort( seg.strokeIndex);	// [2] : id of index of stroke
							helper.ra.writeInt( seg.pivot);			// [4] : pivot
							helper.ra.writeInt( seg.travel);		// [4] : travel
						}
					}
				}
				break;}
			default:
				MDebug.handleWarning(WarningType.STRUCTURAL, this, "Null Image Type.  Attempting to ignore.");
				break;
			}
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
				layers.remove(null);
				helper.ra.writeShort( layers.size());
				
				for( AnimationLayer layer : layers) {
					// [4] : Group Node Bound to
					helper.ra.writeInt((layer.getGroupLink() == null) ? 0 : helper.nodeMap.get(layer.getGroupLink()));
					
					// [1] : bit mask
					helper.ra.writeByte( (layer.includesSubtrees())?1:0);
					
					// [2] : Number of Frames
					List<Frame> linkedFrames = layer.getFrames();
					linkedFrames.removeIf((Frame f) -> {return (f.getLinkedNode() == null);});
					helper.ra.writeShort(linkedFrames.size());
					
					for( Frame frame : linkedFrames) {
						// [4] : NodeID of LayerNode linked to
						helper.ra.writeInt( helper.nodeMap.get(frame.getLinkedNode()));

						// [2] : Length
						helper.ra.writeShort(frame.getLength());
						// [2] : Gap Before
						helper.ra.writeShort(frame.getGapBefore());
						// [2] : Gap After
						helper.ra.writeShort(frame.getGapAfter());
					}
				}
			}
			else if( animation instanceof RigAnimation) {
				RigAnimation rigAnimation = (RigAnimation)animation;
				// [1] : ID
				helper.ra.writeByte(SaveLoadUtil.ANIM_RIG);
				
				// [2] : Number of Sprites
				List<RigAnimLayer> spriteLayers = rigAnimation.getSpriteLayers();
				helper.ra.writeShort(spriteLayers.size());
			
				for( RigAnimLayer spriteLayer : spriteLayers) {
					// [4] : NodeID of Sprite
					helper.ra.writeInt(helper.nodeMap.get(spriteLayer.layer));
					
					// [2] : Number of Parts
					List<Part> parts = spriteLayer.sprite.getParts();
					helper.ra.writeShort(parts.size());
					for( Part part : parts) {
						// [n, UTF8 str] : Part Type Name
						helper.ra.write(SaveLoadUtil.strToByteArrayUTF8(part.getTypeName()));

						List<Pair<Float,PartKeyFrame>> keyFrames = spriteLayer.getPartFrames(part).getKeyFrames();
						// [2] : Number of Key Frames;
						helper.ra.writeShort(keyFrames.size());
						
						for( Pair<Float,PartKeyFrame> keyFrame : keyFrames) {
							helper.ra.writeFloat(keyFrame.getKey());		// [4] time index
							
							helper.ra.writeFloat(keyFrame.getValue().tx);	// [4] translation X
							helper.ra.writeFloat(keyFrame.getValue().ty);	// [4] translation Y
							helper.ra.writeFloat(keyFrame.getValue().sx);	// [4] scale X
							helper.ra.writeFloat(keyFrame.getValue().sy);	// [4] scale Y
							helper.ra.writeFloat(keyFrame.getValue().rot);	// [4] rot
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
