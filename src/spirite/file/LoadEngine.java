package spirite.file;

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

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MUtil;
import spirite.brains.MasterControl;
import spirite.image_data.GroupTree;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageHandle;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.layers.Layer;
import spirite.image_data.layers.SimpleLayer;

/***
 * LoadEngine is a static container for methods which load images from
 * various file formats (but particularly the native SIF format)
 */
public class LoadEngine {
	private static MasterControl master;
	public static void setMaster( MasterControl m) { master = m;}
	
	private static class LoadSettings {
		int version;
	}
	
	/** Attempts to load the given file into a new Workspace. */
	public static ImageWorkspace loadWorkspace( File file) 
		throws BadSIFFFileException
	{
		RandomAccessFile ra;
		LoadSettings settings = new LoadSettings();
		
		try {
			int width = -1;
			int height = -1;
			
			if( !file.exists()) {
				throw new BadSIFFFileException("File Does Not Exist.");
			}
			
			ra = new RandomAccessFile( file, "r");
			
			// Verify Header
			ra.seek(0);
			byte[] header = SaveLoadUtil.getHeader();
			byte[] b = new byte[4];
			ra.read(b);
			
			
			if( !Arrays.equals( header, b)) {
				ra.close();
				throw new BadSIFFFileException("Bad Fileheader (not an SIFF File).");
			}
			
			settings.version = ra.readInt();
			
			if( settings.version >= 1) {
				int packed = ra.readInt();
				width = MUtil.high16(packed);
				height= MUtil.low16(packed);
			}
			
			ImageWorkspace workspace = new ImageWorkspace(master.getCacheManager());
			Map<Integer,BufferedImage> imageMap = new HashMap<>();

			// Load Chunks until you've reached the end
			List<ChunkInfo> chunks = parseChunks( ra);

			// First Load the Image Data
			for( ChunkInfo ci : chunks) {
				if( ci.header.equals("IMGD")) {
					ra.seek( ci.startPointer);
					imageMap = parseImageDataSection(ra, ci.size);
				}
			}
			
			// Next Load the Group Tree
			for( ChunkInfo ci : chunks) {
				if( ci.header.equals("GRPT")) {
					ra.seek( ci.startPointer);
					parseGroupTreeSection(workspace, ra, ci.size,settings);
				}
			}
			
			// TODO DEBUG
			for( BufferedImage img : imageMap.values()) {
				if( img.getWidth() > workspace.getWidth()) {
					workspace.setWidth(img.getWidth());
				}
				if( img.getHeight() > workspace.getHeight()) {
					workspace.setHeight(img.getHeight());
				}
			}
			// TODO
			
			workspace.importData( workspace.getRootNode(), imageMap);

			workspace.setWidth(width);
			workspace.setHeight(height);
			
			workspace.finishBuilding();
			workspace.fileSaved(file);
			return workspace;
			
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
	private static List<ChunkInfo> parseChunks(RandomAccessFile ra) 
			throws IOException 
	{
		List<ChunkInfo> list = new ArrayList<ChunkInfo>();

		byte[] b = new byte[4];
		
		while( ra.read(b) == 4) {
			ChunkInfo ci = new ChunkInfo();
			
			ci.size = ra.readInt();
			ci.header = new String( b, "UTF-8");
			ci.startPointer = ra.getFilePointer();
			ra.skipBytes(ci.size);
			
			list.add(ci);
		}
		
		return list;
	}

	/***
	 * Read ImageData Section Data
	 * [IMGD]
	 */
	private static Map<Integer,BufferedImage> parseImageDataSection(
			RandomAccessFile ra, int chunkSize) 
			throws IOException 
	{
		Map<Integer,BufferedImage> dataMap = new HashMap<>();
		long endPointer = ra.getFilePointer() + chunkSize;
		int identifier;
		int imgSize;
		
		while( ra.getFilePointer() < endPointer) {
			identifier = ra.readInt();
			imgSize = ra.readInt();

			byte[] buffer = new byte[imgSize];
			ra.read(buffer);
			
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(buffer));
			dataMap.put(identifier, img);
			
		}
		
		return dataMap;
	}
	
	/***
	 * Read GroupTree Section Data
	 * [GRPT]
	 */
	private static void parseGroupTreeSection( 
			ImageWorkspace workspace, 
			RandomAccessFile ra, 
			int chunkSize,
			LoadSettings settings) 
			throws IOException 
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
				int packed = ra.readInt();
				ox = MUtil.high16(packed);
				oy = MUtil.low16(packed);
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
				if( type == SaveLoadUtil.NODE_GROUP) {
					node = nodeLayer[depth] = workspace.addGroupNode( nodeLayer[depth-1], name);
					nodeLayer[depth].setExpanded(true);
				}
				if( type == SaveLoadUtil.NODE_SIMPLE_LAYER) {
					Layer layer = new SimpleLayer( new ImageHandle(null, identifier));
					node = workspace.addShellLayer( nodeLayer[depth-1], layer, name);
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
