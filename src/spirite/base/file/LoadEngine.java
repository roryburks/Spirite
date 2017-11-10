package spirite.base.file;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import spirite.base.brains.MasterControl;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.GroupTree;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.MediumHandle;
import spirite.base.image_data.animations.FixedFrameAnimation;
import spirite.base.image_data.animations.FixedFrameAnimation.FrameAbstract;
import spirite.base.image_data.animations.FixedFrameAnimation.Marker;
import spirite.base.image_data.animations.RigAnimation;
import spirite.base.image_data.animations.RigAnimation.PartKeyFrame;
import spirite.base.image_data.animations.RigAnimation.RigAnimLayer;
import spirite.base.image_data.animations.RigAnimation.RigAnimLayer.PartFrames;
import spirite.base.image_data.layers.Layer;
import spirite.base.image_data.layers.ReferenceLayer;
import spirite.base.image_data.layers.SimpleLayer;
import spirite.base.image_data.layers.SpriteLayer;
import spirite.base.image_data.layers.SpriteLayer.Part;
import spirite.base.image_data.layers.SpriteLayer.PartStructure;
import spirite.base.image_data.mediums.DynamicMedium;
import spirite.base.image_data.mediums.FlatMedium;
import spirite.base.image_data.mediums.IMedium;
import spirite.base.image_data.mediums.IMedium.InternalImageTypes;
import spirite.base.image_data.mediums.PrismaticMedium;
import spirite.base.image_data.mediums.maglev.MaglevMedium;
import spirite.base.image_data.mediums.maglev.parts.MagLevFill;
import spirite.base.image_data.mediums.maglev.parts.MagLevFill.StrokeSegment;
import spirite.base.image_data.mediums.maglev.parts.MagLevStroke;
import spirite.base.image_data.mediums.maglev.parts.MagLevThing;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.StrokeEngine;
import spirite.base.pen.StrokeEngine.StrokeParams;
import spirite.hybrid.HybridUtil;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;
import spirite.hybrid.MDebug.WarningType;

/***
 * LoadEngine is a static container for methods which load images from
 * various file formats (but particularly the native SIF format)
 */
public class LoadEngine {
	private final MasterControl master;
	
	public LoadEngine(MasterControl master) {
		this.master = master;
	}
	
	
    public void openFile( File f) {
    	String ext = f.getName().substring( f.getName().lastIndexOf(".")+1);
    	
    	boolean attempted = false;
    	if( ext.equals("png") || ext.equals("bmp") || ext.equals("jpg") || ext.equals("jpeg")) 
    	{
    		// First try to load the file as normal if it's a normal image format
    		try {
    			RawImage img = HybridUtil.load(f);
				master.createWorkspaceFromImage(img, true).fileSaved(f);
				master.getSettingsManager().setImageFilePath(f);
				return;
			} catch (IOException e) {
				attempted = true;
			}
    	}
		try {
			// If it's not recognized (or failed to load) as a normal file, try to
			//	load it as an SIF
			ImageWorkspace ws = master.getLoadEngine().loadWorkspace( f);
			ws.fileSaved(f);
			master.addWorkpace( ws, true);
			master.getSaveEngine().triggerAutosave(ws, 5, 1);	// Autosave every 5 minutes
			master.getSettingsManager().setWorkspaceFilePath(f);
			return;
		} catch (BadSIFFFileException e) {}
		if( !attempted) {
			// If we didn't try to load the image as a normal format already (if 
			//	its extension was not recognized) and loading it as an SIF failed,
			//	try to load it as a normal Image
    		try {
    			RawImage img = HybridUtil.load(f);
				master.createWorkspaceFromImage(img, true).fileSaved(f);
				master.getSettingsManager().setImageFilePath(f);
				return;
			} catch (IOException e) {}
		}
    }
	
    
    
	private static class LoadHelper {
		int version;
		List<ChunkInfo> chunkList;
		RandomAccessFile ra;
		ImageWorkspace workspace;
		
//		int layerMet = 0;
		final List<Node> nodes = new ArrayList<>();
	}
	
	/** Attempts to load the given file into a new Workspace. */
	public ImageWorkspace loadWorkspace( File file) 
		throws BadSIFFFileException
	{
		LoadHelper helper = new LoadHelper();
		
		try {
			int width = -1;
			int height = -1;
			
			if( !file.exists()) {
				throw new BadSIFFFileException("File Does Not Exist.");
			}
			
			helper.ra = new RandomAccessFile( file, "r");
			
			// Verify Header
			helper.ra.seek(0);
			byte[] header = SaveLoadUtil.getHeader();
			byte[] b = new byte[4];
			helper.ra.read(b);
			
			
			if( !Arrays.equals( header, b)) {
				helper.ra.close();
				throw new BadSIFFFileException("Bad Fileheader (not an SIFF File).");
			}
			
			helper.version = helper.ra.readInt();
			
			if( helper.version >= 1) {
				width = helper.ra.readShort();
				height= helper.ra.readShort();
			}
			
			helper.workspace = new ImageWorkspace(master);
			Map<Integer,IMedium> imageMap = new HashMap<>();

			// Load Chunks until you've reached the end
			parseChunks( helper);

			// First Load the Image Data
			for( ChunkInfo ci : helper.chunkList) {
				if( ci.header.equals("IMGD")) {
					helper.ra.seek( ci.startPointer);
					imageMap = parseImageDataSection(helper, ci.size);
				}
			}
			
			// Next Load the Group Tree
			for( ChunkInfo ci : helper.chunkList ) {
				if( ci.header.equals("GRPT")) {
					helper.ra.seek( ci.startPointer);
					parseGroupTreeSection(helper, ci.size);
				}
			}

			// Next Load the Animation Data
			if( helper.version >= 3) {
				for( ChunkInfo ci : helper.chunkList ) {
					if( ci.header.equals("ANIM")) {
						helper.ra.seek( ci.startPointer);
						parseAnimationSection(helper, ci.size);
					}
				}
			}
			helper.ra.close();
			
			if( helper.version < 2) {
				for( IMedium img : imageMap.values()) {
					if( img.getWidth() > helper.workspace.getWidth()) {
						helper.workspace.setWidth(img.getWidth());
					}
					if( img.getHeight() > helper.workspace.getHeight()) {
						helper.workspace.setHeight(img.getHeight());
					}
				}
			}
			
			helper.workspace.importNodeWithData( helper.workspace.getRootNode(), imageMap);

			helper.workspace.setWidth(width);
			helper.workspace.setHeight(height);
			
			helper.workspace.finishBuilding();
			helper.workspace.fileSaved(file);
			return helper.workspace;
			
		} catch (UnsupportedEncodingException e) {
			MDebug.handleError(ErrorType.FILE, null, "UTF-8 Format Unsupported (somehow).");
			throw new BadSIFFFileException("UTF-8 Format Unsupported (somehow).");
		}catch( IOException e) {
			throw new BadSIFFFileException("Error Reading File: " + e.getStackTrace());
		}
	}



	private static class ChunkInfo {
		String header;
		long startPointer;
		int size;
	}
	
	/***
	 * Reads all the header data from the chunks
	 * 
	 * Note: assumes that the file is already aligned to the first chunk.
	 */
	private void parseChunks(LoadHelper helper) 
			throws IOException 
	{
		helper.chunkList = new ArrayList<ChunkInfo>();

		byte[] b = new byte[4];
		
		while( helper.ra.read(b) == 4) {
			ChunkInfo ci = new ChunkInfo();
			
			ci.size = helper.ra.readInt();
			ci.header = new String( b, "UTF-8");
			ci.startPointer = helper.ra.getFilePointer();
			helper.ra.skipBytes(ci.size);
			
			helper.chunkList.add(ci);
		}
	}

	/***
	 * Read ImageData Section Data
	 * [IMGD]
	 */
	private Map<Integer, IMedium> parseImageDataSection(
			LoadHelper helper, int chunkSize) 
			throws IOException 
	{
		Map<Integer,IMedium> dataMap = new HashMap<>();
		long endPointer = helper.ra.getFilePointer() + chunkSize;
		int identifier;
		
		while( helper.ra.getFilePointer() < endPointer) {
			identifier = helper.ra.readInt();
			
			int typeId = (helper.version <4)?0:helper.ra.readByte();
			InternalImageTypes type = InternalImageTypes.values()[typeId];
			
			switch( type) {
			case NORMAL: {
				int imgSize = helper.ra.readInt();
				
				byte[] buffer = new byte[imgSize];
				helper.ra.read(buffer);
				RawImage img = HybridUtil.load(new ByteArrayInputStream(buffer));

				dataMap.put(identifier, new FlatMedium(img,helper.workspace));
				break;}
			case DYNAMIC: {
				int ox = helper.ra.readShort();
				int oy = helper.ra.readShort();
				int imgSize = helper.ra.readInt();
				
				byte[] buffer = new byte[imgSize];
				helper.ra.read(buffer);
				RawImage img = HybridUtil.load(new ByteArrayInputStream(buffer));

				dataMap.put(identifier, new DynamicMedium(img,ox,oy,helper.workspace));
				break;}
			case PRISMATIC: {
				int colorCount = helper.ra.readShort();
				
				List<PrismaticMedium.LImg> loadingList = new ArrayList<>(colorCount);
				for( int i=0; i<colorCount; ++i) {
					PrismaticMedium.LImg limg = new PrismaticMedium.LImg();

					limg.color = helper.ra.readInt();
					limg.ox = helper.ra.readShort();
					limg.oy = helper.ra.readShort();
					int imgSize = helper.ra.readInt();
					
					byte[] buffer = new byte[imgSize];
					helper.ra.read(buffer);
					limg.img = HybridUtil.load(new ByteArrayInputStream(buffer));

					loadingList.add(limg);
				}
				
				dataMap.put(identifier, new PrismaticMedium(loadingList));				
				break;}
			case MAGLEV: {
				int thingsLeftToRead = helper.ra.readUnsignedShort();
				
				List<MagLevThing> things = new ArrayList<>(thingsLeftToRead);
				
				for( ; thingsLeftToRead != 0; --thingsLeftToRead) {
					int thingType = helper.ra.readByte();
					
					switch( thingType) {
					case 0:{	// Stroke
						int color = helper.ra.readInt();
						StrokeEngine.Method method = StrokeEngine.Method.fromFileId(helper.ra.readUnsignedByte());
						float width = helper.ra.readFloat();
						int numPenStatesToRead = helper.ra.readUnsignedShort();
						
						PenState[] pss = new PenState[numPenStatesToRead];
						for( int i=0; i < numPenStatesToRead; ++i) {
							float x = helper.ra.readFloat();
							float y = helper.ra.readFloat();
							float p = helper.ra.readFloat();
							
							pss[i] = new PenState(x, y, p);
						}
						StrokeParams params = new StrokeParams();
						params.setWidth(width);
						params.setMethod(method);
						params.setColor(color);
						
						things.add(new MagLevStroke(pss, params));
						
						break;}
					case 1:{	// Fill
						int color = helper.ra.readInt();
						int numSegmentsToRead = helper.ra.readUnsignedShort();
						
						List<StrokeSegment> segments = new ArrayList<>(numSegmentsToRead);
						
						for( ; numSegmentsToRead > 0; --numSegmentsToRead) {
							StrokeSegment ss = new StrokeSegment();
							
							ss.strokeIndex = helper.ra.readUnsignedShort();
							ss.pivot = helper.ra.readInt();
							ss.travel = helper.ra.readInt();
							
							segments.add(ss);
						}

						things.add( new MagLevFill(segments, color));
						
						break;}
					}
				}
				

				dataMap.put(identifier, new MaglevMedium(helper.workspace, things));	
				
				break;}
			}
		}
		
		return dataMap;
	}
	
	/***
	 * Read GroupTree Section Data
	 * [GRPT]
	 */
	private void parseGroupTreeSection( 
			LoadHelper helper,
			int chunkSize) 
			throws IOException 
	{

		if( helper.version <= 1) {
			_LEGACY_HandleGroupTree0001(helper.workspace, helper.ra, chunkSize, helper);
			return;
		}
		
		long endPointer = helper.ra.getFilePointer() + chunkSize;
		int depth = 0;
		int type;
		int identifier = -1;
		String name;
		
		// Create a array that keeps track of the active layers of group
		//	nodes (all the nested nodes leading up to the current node)
		GroupTree.GroupNode[] nodeLayer = new GroupTree.GroupNode[256];
		
		nodeLayer[0] = helper.workspace.getRootNode();
		for( int i = 1; i < 256; ++i) {
			nodeLayer[i] = null;
		}
		
		Map<LayerNode, Integer> referencesToMap = new HashMap<>();
		
		while( helper.ra.getFilePointer() < endPointer) {
			
			// Default values
			float alpha = 1.0f;
			int ox = 0;
			int oy = 0;
			int bitmask = 0x01 | 0x02;
			
			// Read data
			depth = helper.ra.readUnsignedByte();
			
			if( helper.version >= 1) {
				alpha = helper.ra.readFloat();
				ox = helper.ra.readShort();
				oy = helper.ra.readShort();
				bitmask = helper.ra.readByte();
			}

			name = SaveLoadUtil.readNullTerminatedStringUTF8(helper.ra);
			type = helper.ra.readUnsignedByte();
			
			
			
			Node node = null;
			
			// !!!! Kind of hack-y that it's even saved, but only the root node should be
			//	depth 0 and there should only be one (and it's already created)
			if( depth == 0) {
				helper.nodes.add(helper.workspace.getRootNode());
				continue;
			}
			else {
				switch( type) {
				case SaveLoadUtil.NODE_GROUP:
					node = nodeLayer[depth] = helper.workspace.addGroupNode( nodeLayer[depth-1], name);
					nodeLayer[depth].setExpanded(true);
					break;
				case SaveLoadUtil.NODE_SIMPLE_LAYER:
					identifier = helper.ra.readInt();
					Layer layer = new SimpleLayer( new MediumHandle(null, identifier));
					node = helper.workspace.addShellLayer( nodeLayer[depth-1], layer, name);
					break;
				case SaveLoadUtil.NODE_RIG_LAYER: {
					int partCount = helper.ra.readByte();
					List<PartStructure> parts = new ArrayList<>( partCount);
					
					
					for( int i=0; i<partCount; ++i) {
						PartStructure part = new PartStructure();
						if( helper.version <= 4) {
							part.partName = SaveLoadUtil.readNullTerminatedStringUTF8(helper.ra);
							
							part.transX = helper.ra.readShort();
							part.transY =  helper.ra.readShort();
							part.depth = helper.ra.readInt();
							
							int pid = helper.ra.readInt();
							part.handle = new MediumHandle(null, pid);
						}
						else {
							part.partName = SaveLoadUtil.readNullTerminatedStringUTF8(helper.ra);

							part.transX = helper.ra.readFloat();
							part.transY = helper.ra.readFloat();
							part.scaleX = helper.ra.readFloat();
							part.scaleY = helper.ra.readFloat();
							part.rot = helper.ra.readFloat();
							part.depth = helper.ra.readInt();
							int pid = helper.ra.readInt();
							part.handle = new MediumHandle( null, pid);
						}
						
						parts.add(part);
					}
					
					
					SpriteLayer rig = new SpriteLayer( parts);
					node = helper.workspace.addShellLayer(nodeLayer[depth-1], rig, name);
					break;}
				case SaveLoadUtil.NODE_REFERENCE_LAYER: {
					node = helper.workspace.addNewReferenceLayer(nodeLayer[depth-1], null, "");
					referencesToMap.put((LayerNode)node, helper.ra.readInt());
					//LayerNode node 
					//node = helper.workspace.addShellLayer(nodeLayer[depth-1], new ReferenceLayer(helper.nodes.get(helper.ra.readInt())), name)
//						helper.w
//						// [4] : NodeID of referenced node
//						helper.ra.writeInt(helper.nodeMap.get(((ReferenceLayer)layer).underlying));
					break;}
				}
			}
			if( node != null) {
				helper.nodes.add(node);
				node.getRender().setAlpha(alpha);
				
				node.setExpanded( (bitmask & SaveLoadUtil.EXPANDED_MASK) != 0);
				node.getRender().setVisible( (bitmask & SaveLoadUtil.VISIBLE_MASK) != 0);
				node.setOffset(ox, oy);
			}
		}
		
		// Link the reference nodes (needs to be done afterwards because it might link to a node yet
		//	added to the node map since nodeIDs are based on depth-first Group Tree order, 
		//	not creation order)
		for(Entry<LayerNode,Integer> entry :  referencesToMap.entrySet()) {
			((ReferenceLayer)entry.getKey().getLayer()).setUnderlying((LayerNode) helper.nodes.get(entry.getValue()));
		}
	}
	

	
	private void parseAnimationSection(LoadHelper helper, int chunkSize) 
			throws IOException 
	{
		long endPointer = helper.ra.getFilePointer() + chunkSize;
		
		while( helper.ra.getFilePointer() < endPointer) {
			String name = SaveLoadUtil.readNullTerminatedStringUTF8(helper.ra);
			
			int type = helper.ra.readByte();
			
			if( type == SaveLoadUtil.ANIM_FIXED_FRAME) {
				if( helper.version < 8)
					_LEGACY_loadFFA0007(helper, name);
				else
					loadFixedFrameAnimation( helper, name);
			}
			else if( type == SaveLoadUtil.ANIM_RIG)
				loatRigAnimation( helper, name);
			else {
				MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Unrecognized Animation Type: " + type);
				return;
			}
		}
	}
	
//    [2, ushort] : Number of Layers
//    Per Layer:
//        [4, int] : NodeID of GroupNode Bound to (see Bellow), can be -1 for unbounded
//		  [1, byte] : 0 bit: whether or not subgroups are linked
//        [2, ushort] : Number of Frames that have information
//        Per Frame:
//            [4, byte] : NodeID 
//				(note, if it's a LayerNode it's a FrameFrame.  if it's a GroupNode it's a Start_of_loop frame)
//            [2, ushort] : Length
//			[2, ushort] : Gap Before
//			[2, ushort] : Gap After
	private void loadFixedFrameAnimation( LoadHelper helper, String name) 
			throws IOException 
	{
		FixedFrameAnimation animation = new FixedFrameAnimation(name, helper.workspace);
		
		int layerCount = helper.ra.readUnsignedShort();
		
		for( int i=0; i<layerCount; ++i) {
			Map<Node,FrameAbstract> nodeMap = new HashMap<>();
			
			int groupNodeID = helper.ra.readInt();
			int mask = helper.ra.readByte();
			int frameCount = helper.ra.readUnsignedShort();
			
			
			GroupNode linkedGroup = ( groupNodeID > 0) ? (GroupNode) helper.nodes.get(groupNodeID) : null;
			boolean usesSubgroups = ((mask & 1) == 1);
			
			for( int j=0; j<frameCount; ++j) {
				Node node = helper.nodes.get(helper.ra.readInt());
				int length = helper.ra.readUnsignedShort();
				int gapBefore = helper.ra.readUnsignedShort();
				int gapAfter = helper.ra.readUnsignedShort();

				if( node instanceof LayerNode)
					nodeMap.put( node, new FrameAbstract( node, length, Marker.FRAME, gapBefore, gapAfter));
				if( node instanceof GroupNode)
					nodeMap.put( node, new FrameAbstract( node, length, Marker.START_LOCAL_LOOP, gapBefore, gapAfter));
			}
			
			animation.addBuiltLinkedLayer(linkedGroup, nodeMap, usesSubgroups);
		}
		helper.workspace.getAnimationManager().addAnimation(animation);
	}
	
	private void loatRigAnimation( LoadHelper helper, String name) 
			throws IOException 
	{
		RigAnimation animation = new RigAnimation(helper.workspace, name);
		
		// [2] : Number of Sprites
		int numSprites = helper.ra.readUnsignedShort();
		
		for( int i=0; i<numSprites; ++i) {
			int spriteNodeId = helper.ra.readInt();			// [4] : NodeID of Sprite
			int numParts = helper.ra.readUnsignedShort();	// [2] : Number of Parts
			
			RigAnimLayer rail = animation.addSprite((LayerNode) helper.nodes.get(spriteNodeId));
			SpriteLayer sprite = (SpriteLayer)((LayerNode) helper.nodes.get(spriteNodeId)).getLayer();
			List<Part> parts = sprite.getParts();
			
			for( int p=0; p<numParts; ++p) {
				String partName = SaveLoadUtil.readNullTerminatedStringUTF8(helper.ra);	// [n] : Part Type Name
				int numKeyFrames = helper.ra.readUnsignedShort();	// [2] : Number of Key Frames
				
				Part part = null;
				for( Part find : parts) {if( find.getTypeName().equals(partName)) part = find;}
				
				PartFrames partFrames = rail.getPartFrames(part);
				
				for( int k=0; k<numKeyFrames; ++k) {
					float t = helper.ra.readFloat();
					PartKeyFrame keyframe = new PartKeyFrame(
							helper.ra.readFloat(),	//tx
							helper.ra.readFloat(),	//ty
							helper.ra.readFloat(),	//sx
							helper.ra.readFloat(),	//sy
							helper.ra.readFloat());	//rot
					
					partFrames.addKeyFrame(t, keyframe);
				}
			}
		}
		helper.workspace.getAnimationManager().addAnimation(animation);
	}
	
	// Legacy Methods: Handles conversion of depreciated formats into new standards
	//	without cluttering code too much
	private static void _LEGACY_HandleGroupTree0001(
			ImageWorkspace workspace, 
			RandomAccessFile ra,
			int chunkSize,
			LoadHelper settings) throws IOException 
	{
		long endPointer = ra.getFilePointer() + chunkSize;
		int depth = 0;
		int type;
		int identifier = -1;
		String name;
		
		// Create a array that keeps track of the active layers of group
		//	nodes (all the nested nodes leading up to the current node)
		GroupTree.GroupNode[] nodeLayer = new GroupTree.GroupNode[256];
		
		nodeLayer[0] = workspace.getRootNode();
		for( int i = 1; i < 256; ++i) {
			nodeLayer[i] = null;
		}
		
		while( ra.getFilePointer() < endPointer) {
			
			// Default values
			float alpha = 1.0f;
			int ox = 0;
			int oy = 0;
			int bitmask = 0x01 | 0x02;
			
			// Read data
			depth = ra.readUnsignedByte();
			
			if( settings.version >= 1) {
				alpha = ra.readFloat();
				ox = ra.readShort();
				oy = ra.readShort();
				bitmask = ra.readByte();
			}
			
			type = ra.readUnsignedByte();
			
			if( type == SaveLoadUtil.NODE_SIMPLE_LAYER) 
				identifier = ra.readInt();
			
			
			name = SaveLoadUtil.readNullTerminatedStringUTF8(ra);
			
			Node node = null;
			
			// !!!! Kind of hack-y that it's even saved, but only the root node should be
			//	depth 0 and there should only be one (and it's already created)
			if( depth == 0) { continue;}
			else {
				switch( type) {
				case SaveLoadUtil.NODE_GROUP:
					node = nodeLayer[depth] = workspace.addGroupNode( nodeLayer[depth-1], name);
					nodeLayer[depth].setExpanded(true);
					break;
				case SaveLoadUtil.NODE_SIMPLE_LAYER:
					Layer layer = new SimpleLayer( new MediumHandle(null, identifier));
					node = workspace.addShellLayer( nodeLayer[depth-1], layer, name);
					break;
				}
			}
			if( node != null) {
				node.getRender().setAlpha(alpha);
				
				node.setExpanded( (bitmask & SaveLoadUtil.EXPANDED_MASK) != 0);
				node.getRender().setVisible( (bitmask & SaveLoadUtil.VISIBLE_MASK) != 0);
				node.setOffset(ox, oy);
			}
		}
	}

	private void _LEGACY_loadFFA0007( LoadHelper helper, String name) 
			throws IOException 
	{
		FixedFrameAnimation animation = new FixedFrameAnimation(name, helper.workspace);
		
		int layerCount = helper.ra.readUnsignedShort();
		
		for( int i=0; i<layerCount; ++i) {
			Map<Node,FrameAbstract> nodeMap = new HashMap<>();
			
			int groupNodeID = helper.ra.readInt();
			int frameCount = helper.ra.readUnsignedShort();
			
			GroupNode linkedGroup = null;
			if( groupNodeID > 0) {
				linkedGroup = (GroupNode) helper.nodes.get(groupNodeID);
			}
			for( int j=0; j<frameCount; ++j) {
				int marker = helper.ra.readByte();
				int length = helper.ra.readShort();
				int nodeLink = (marker == 0) ? helper.ra.readInt() : 0;
				
				if( nodeLink > 0) {
					Node node = helper.nodes.get(nodeLink);
					nodeMap.put( node, new FrameAbstract( (LayerNode) node, length, Marker.FRAME, 0, 0));
				}
			}
			
			animation.addBuiltLinkedLayer(linkedGroup, nodeMap, false);
		}
		helper.workspace.getAnimationManager().addAnimation(animation);
	}
	
	public static class BadSIFFFileException extends Exception {
		private static final long serialVersionUID = 1L;

		public BadSIFFFileException( String message) {
			super(message);
		}
	}
}
