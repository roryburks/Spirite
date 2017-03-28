package spirite.file;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
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

import javax.imageio.ImageIO;

import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.MUtil;
import spirite.brains.MasterControl;
import spirite.image_data.GroupTree;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageHandle;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.DynamicImportImage;
import spirite.image_data.ImageWorkspace.ImportImage;
import spirite.image_data.animation_data.FixedFrameAnimation;
import spirite.image_data.animation_data.FixedFrameAnimation.AnimationLayerBuilder;
import spirite.image_data.animation_data.FixedFrameAnimation.Marker;
import spirite.image_data.layers.Layer;
import spirite.image_data.layers.SimpleLayer;
import spirite.image_data.layers.SpriteLayer;
import spirite.image_data.layers.SpriteLayer.Part;

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
				BufferedImage bi = ImageIO.read(f);
				BufferedImage bi2 = new BufferedImage( bi.getWidth(), bi.getHeight(), Globals.BI_FORMAT);
				MUtil.clearImage(bi2);
				Graphics g = bi2.getGraphics();
				g.drawImage(bi, 0, 0, null);
				g.dispose();
				master.createWorkspaceFromImage(bi2, true).fileSaved(f);
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
			master.getSaveEngine().triggerAutosave(ws, 5*60, 10);	// Autosave every 5 minutes
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
			Map<Integer,ImportImage> imageMap = new HashMap<>();

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
				for( ImportImage impi : imageMap.values()) {
					BufferedImage img = impi.image;
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
	private Map<Integer, ImportImage> parseImageDataSection(
			LoadHelper helper, int chunkSize) 
			throws IOException 
	{
		Map<Integer,ImportImage> dataMap = new HashMap<>();
		long endPointer = helper.ra.getFilePointer() + chunkSize;
		int identifier;
		int imgSize;
		int mask = 0;
		int ox = 0;
		int oy = 0;
		
		while( helper.ra.getFilePointer() < endPointer) {
			identifier = helper.ra.readInt();
			if( helper.version >= 4) {
				mask = helper.ra.readByte();
				if( ((mask & SaveLoadUtil.DYNMIC_MASK) != 0)) {
					ox = helper.ra.readShort();
					oy = helper.ra.readShort();
				}
			}

			imgSize = helper.ra.readInt();
			byte[] buffer = new byte[imgSize];
			helper.ra.read(buffer);
			
			BufferedImage bi = ImageIO.read(new ByteArrayInputStream(buffer));
			BufferedImage bi2 = new BufferedImage( bi.getWidth(), bi.getHeight(), Globals.BI_FORMAT);
			MUtil.clearImage(bi2);
			Graphics g = bi2.getGraphics();
			g.drawImage(bi, 0, 0, null);
			g.dispose();
			
			ImportImage impi;
			if( ((mask & SaveLoadUtil.DYNMIC_MASK) != 0)) {
				 impi = new DynamicImportImage( bi2, ox, oy);
			}
			else
				impi = new ImportImage( bi2);
			dataMap.put(identifier, impi);
			
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
			_legacyHandleGroupTree0001(helper.workspace, helper.ra, chunkSize, helper);
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
					Layer layer = new SimpleLayer( new ImageHandle(null, identifier));
					node = helper.workspace.addShellLayer( nodeLayer[depth-1], layer, name);
					break;
				case SaveLoadUtil.NODE_RIG_LAYER: {
					int partCount = helper.ra.readByte();
					List<Part> parts = new ArrayList<Part>( partCount);
					
					
					for( int i=0; i<partCount; ++i) {
						String partName = SaveLoadUtil.readNullTerminatedStringUTF8(helper.ra);
						int pox = helper.ra.readShort();
						int poy = helper.ra.readShort();
						int pdepth = helper.ra.readInt();
						int pid = helper.ra.readInt();
						
						parts.add( new Part( 
								new ImageHandle( null, pid),
								partName,
								pox, poy, pdepth,
								true, 1.0f));
					}
					
					
					SpriteLayer rig = new SpriteLayer( parts);
					node = helper.workspace.addShellLayer(nodeLayer[depth-1], rig, name);
					break;}
				}
			}
			if( node != null) {
				helper.nodes.add(node);
				node.setAlpha(alpha);
				
				node.setExpanded( (bitmask & SaveLoadUtil.EXPANDED_MASK) != 0);
				node.setVisible( (bitmask & SaveLoadUtil.VISIBLE_MASK) != 0);
				node.setOffset(ox, oy);
			}
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
				loadFixedFrameAnimation( helper, name);
			}
			else {
				MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Unrecognized Animation Type: " + type);
				return;
			}
		}
	}
	
	private void loadFixedFrameAnimation( LoadHelper helper, String name) 
			throws IOException 
	{
		FixedFrameAnimation animation = new FixedFrameAnimation(name);
		
		int layerCount = helper.ra.readShort();
		
		for( int i=0; i<layerCount; ++i) {
			AnimationLayerBuilder builder = new AnimationLayerBuilder();
			
			int groupNodeID = helper.ra.readInt();
			int frameCount = helper.ra.readShort();
			
			if( groupNodeID > 0)
				builder.setGroupLink((GroupNode) helper.nodes.get(groupNodeID));
			
			for( int j=0; j<frameCount; ++j) {
				Marker marker = Marker.values()[helper.ra.readByte()];
				int length = helper.ra.readShort();
				LayerNode node = (marker == Marker.FRAME) 
						? (LayerNode)helper.nodes.get(helper.ra.readInt())
						: null;
				
				builder.addFrame(marker, length, node);
			}
			
			animation.addBuiltLayer(builder);
		}
		helper.workspace.getAnimationManager().addAnimation(animation);
	}
	
	// Legacy Methods: Handles conversion of depreciated formats into new standards
	//	without cluttering code too much
	private static void _legacyHandleGroupTree0001(
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
					Layer layer = new SimpleLayer( new ImageHandle(null, identifier));
					node = workspace.addShellLayer( nodeLayer[depth-1], layer, name);
					break;
				}
			}
			if( node != null) {
				node.setAlpha(alpha);
				
				node.setExpanded( (bitmask & SaveLoadUtil.EXPANDED_MASK) != 0);
				node.setVisible( (bitmask & SaveLoadUtil.VISIBLE_MASK) != 0);
				node.setOffset(ox, oy);
			}
		}
	}
	
	public static class BadSIFFFileException extends Exception {
		private static final long serialVersionUID = 1L;

		public BadSIFFFileException( String message) {
			super(message);
		}
	}
}
