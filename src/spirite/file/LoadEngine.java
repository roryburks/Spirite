package spirite.file;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageData;
import spirite.image_data.ImageWorkspace;

public class LoadEngine {

	
	public static ImageWorkspace loadWorkspace( File file) 
		throws BadSIFFFileException
	{
		RandomAccessFile ra;
		
		try {
			if( !file.exists()) {
				throw new BadSIFFFileException("File Does Not Exist.");
			}
			
			ra = new RandomAccessFile( file, "r");
			
			// Verify Header
			ra.seek(0);
			byte[] header = SaveEngine.getHeader();
			byte[] b = new byte[8];
			ra.read(b);
			
			if( !Arrays.equals( header, b)) {
				ra.close();
				throw new BadSIFFFileException("Bad Fileheader (not an SIFF File).");
			}
			
			ImageWorkspace workspace = new ImageWorkspace();
			
			// Load Chunks until you've reached the end
			List<ChunkInfo> chunks = parseChunks( ra);

			// First Load the Image Data
			for( ChunkInfo ci : chunks) {
				if( ci.header.equals("IMGD")) {
					ra.seek( ci.startPointer);
					parseImageDataSection(workspace, ra, ci.size);
				}
			}
			
			// Next Load the Group Tree
			for( ChunkInfo ci : chunks) {
				if( ci.header.equals("GRPT")) {
					ra.seek( ci.startPointer);
					parseGroupTreeSection(workspace, ra, ci.size);
				}
			}
			
			workspace.resetUndoEngine();
			return workspace;
			
		} catch (UnsupportedEncodingException e) {
			MDebug.handleError(ErrorType.FILE, null, "UTF-8 Format Unsupported (somehow).");
			throw new BadSIFFFileException("UTF-8 Format Unsupported (somehow).");
		}catch( IOException e) {
//			e.printStackTrace();
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
	private static void parseImageDataSection(
			ImageWorkspace workspace, RandomAccessFile ra, int chunkSize) 
			throws IOException 
	{
		long startPointer = ra.getFilePointer();
		long endPointer = ra.getFilePointer() + chunkSize;
		int identifier;
		int imgSize;
		
		while( ra.getFilePointer() < endPointer) {
			identifier = ra.readInt();
			imgSize = ra.readInt();

			byte[] buffer = new byte[imgSize];
			ra.read(buffer);
			
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(buffer));
			
			ImageData idata = new ImageData( img, identifier, workspace);
			workspace.addImageDataDirect(idata);
		}
	}
	
	/***
	 * Read GroupTree Section Data
	 * [GRPT]
	 */
	private static void parseGroupTreeSection( 
			ImageWorkspace workspace, RandomAccessFile ra, int chunkSize) 
			throws IOException 
	{
		long startPointer = ra.getFilePointer();
		long endPointer = ra.getFilePointer() + chunkSize;
		int layer = 0;
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
			// Read data
			layer = ra.readUnsignedByte();			
			type = ra.readUnsignedByte();
			
			if( type == SaveLoadUtil.NODE_LAYER) 
				identifier = ra.readInt();
			
			name = SaveLoadUtil.readNullTerminatedStringUTF8(ra);
			
			// !!!! Kind of hack-y that it's even saved, but only the root node should be
			//	layer 0 and there should only be one (and it's already created)
			if( layer == 0) {}
			else {
				if( type == SaveLoadUtil.NODE_GROUP) {
					nodeLayer[layer] = workspace.addTreeNode( nodeLayer[layer-1], name);
					nodeLayer[layer].setExpanded(true);
				}
				if( type == SaveLoadUtil.NODE_LAYER) {
					// !!!! TODO DEBUG
					workspace.addNewRig( nodeLayer[layer-1], identifier, name);
				}
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
