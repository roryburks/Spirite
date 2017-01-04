package spirite.file;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageWorkspace;

public class LoadEngine {
	/***
	 * Reads the given RandomAccessFile for bytes until it reaches a null
	 * string then converts the byte-array (minus the null string) into
	 * a string using UTF-8 format
	 * @throws IOException 
	 */
	public static String readNullTerminatedStringUTF8( RandomAccessFile ra)
			throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		byte b = ra.readByte();
		
		while( b != 0x00) {
			bos.write(b);
			b = ra.readByte();
		}
		
		return bos.toString("UTF-8");
	}
	
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
				throw new BadSIFFFileException("Bad Fileheader (not an SIFF File).");
			}
			
			ImageWorkspace workspace = new ImageWorkspace();
			
			// Load Chunks until you've reached the end
			while( parseHeader( workspace, ra)) {}
			
			return workspace;
			
		} catch (UnsupportedEncodingException e) {
			MDebug.handleError(ErrorType.FILE, null, "UTF-8 Format Unsupported (somehow).");
			throw new BadSIFFFileException("UTF-8 Format Unsupported (somehow).");
		}catch( IOException e) {
			throw new BadSIFFFileException("Error Reading File: " + e.getMessage());
		}
	}
	private static boolean parseHeader( ImageWorkspace workspace, RandomAccessFile ra) 
			throws IOException
	{
		byte[] b = new byte[4];
		
		if( ra.read(b) < 4) return false;
		
		int chunkSize = ra.readInt();
		
		
		
		if( Arrays.equals( b, "GRPT".getBytes("UTF-8"))) {
			parseGroupTreeSection( workspace, ra, chunkSize);
		}else if( Arrays.equals( b, "IMGD".getBytes("UTF-8"))) {
			ra.skipBytes(chunkSize);
			
		}else {
			MDebug.handleWarning(WarningType.UNSUPPORTED, null, 
					"Unsupported Chunk Type in SIFF File:" + new String( b, "UTF-8"));
			ra.skipBytes(chunkSize);
		}
		
		return true;
	}
	private static void parseGroupTreeSection( 
			ImageWorkspace workspace, RandomAccessFile ra, int chunkSize) 
			throws IOException 
	{
		long startPointer = ra.getFilePointer();
		long endPointer = ra.getFilePointer() + chunkSize;
		int layer = 0;
		int type;
		int identifier;
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
			
//			if( type == SaveLoadUtil.NODE_LAYER) 
	//			identifier = ra.readUnsignedByte();
			
			name = readNullTerminatedStringUTF8(ra);
			
			// !!!! Kind of hack-y that it's even saved, but only the root node should be
			//	layer 0 and there should only be one (and it's already created)
			if( layer == 0) {
				
			}
			
			else {
				if( type == SaveLoadUtil.NODE_GROUP) {
					System.out.print( nodeLayer[layer-1].getChildren().size() + ",");
					nodeLayer[layer] = workspace.addTreeNode( nodeLayer[layer-1], name);
					System.out.println( nodeLayer[layer-1].getChildren().size());
				}
				if( type == SaveLoadUtil.NODE_LAYER) {
					// !!!! TODO DEBUG
					workspace.addNewRig( nodeLayer[layer-1], 128, 128, name, new Color(0,0,0,0));
				}
			}
		}
	}
	
	public static class BadSIFFFileException extends Exception {
		public BadSIFFFileException( String message) {
			super(message);
		}
	}
}
