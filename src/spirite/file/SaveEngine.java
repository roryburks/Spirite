package spirite.file;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.Part;

public class SaveEngine {

	public static byte[] strToNullTerminatedByteUTF8( String str) 
			throws UnsupportedEncodingException
	{
		byte b[] = (str + "\0").getBytes("UTF-8");
		
		// Convert non-terminating null characters to whitespace
		for( int i = 0; i < b.length-1; ++i) {
			if( b[i] == 0x00)
				b[i] = 0x20;
		}
		
		return b;
	}
	
	public static byte[] getHeader() 
		throws UnsupportedEncodingException
	{
		byte b[] = new byte[8];
		System.arraycopy( "SIFF".getBytes("UTF-8"), 0, b, 0, 4);
		
		// Versioning
		b[4] = 0;
		b[5] = 0;
		b[6] = 0;
		b[7] = 0;
		return b;
	}
	
	public static void saveWorkspace( ImageWorkspace workspace, File file) {
		RandomAccessFile ra;
		
		try {
			if( file.exists()) {
				file.delete();
			}
			file.createNewFile();
			
			ra = new RandomAccessFile(file, "rw");
	
			
			
			// Write Header
			ra.write( getHeader());
			
			// Save Group
			saveGroupTree( workspace.getRootNode(), ra);
			
			// Save Image Data
			saveImageData( workspace.getImageData(), ra);
			

			ra.close();
		}catch (UnsupportedEncodingException e) {
			MDebug.handleError(ErrorType.FILE, null, "UTF-8 Format Unsupported (somehow).");
		}catch( IOException e) {
			
		}
	}
	
	/***
	 * 
	 */
	private static void saveGroupTree( GroupTree.Node root, RandomAccessFile ra) 
		throws UnsupportedEncodingException, IOException
	{
		byte depth = 0;
		ra.write( "GRPT".getBytes("UTF-8"));
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		_sgt_rec( root, bos, depth);
		
		ra.writeInt( bos.size());
		ra.write(bos.toByteArray());
	}
	private static void _sgt_rec( GroupTree.Node node, ByteArrayOutputStream bos, byte depth) 
			throws UnsupportedEncodingException, IOException 
	{
		if( node instanceof GroupTree.GroupNode) {
			GroupTree.GroupNode gnode = (GroupTree.GroupNode) node;
			
			bos.write( depth);
			bos.write( SaveLoadUtil.NODE_GROUP);
			
			bos.write( strToNullTerminatedByteUTF8(gnode.getName()));
			
			
			for( GroupTree.Node child : node.getChildren()) {
				if( depth == 0xFF)
					MDebug.handleWarning( WarningType.STRUCTURAL, null, "Too many nested groups (255 limit).");
				else {
					_sgt_rec( child, bos, (byte) (depth+1));
				}
			}
		}
		else if( node instanceof GroupTree.LayerNode) {
			GroupTree.LayerNode rnode = (GroupTree.LayerNode) node;
			
			bos.write( depth);
			bos.write( SaveLoadUtil.NODE_LAYER);
			
			bos.write( strToNullTerminatedByteUTF8( rnode.getLayer().getName()));
		}
		else {
			MDebug.handleWarning(WarningType.STRUCTURAL, null, "Unknown GroupTree Node type on saving.");
		}
	}
	
	private static void saveImageData( List<Part> imageData, RandomAccessFile ra) 
			throws UnsupportedEncodingException, IOException 
	{
		ra.write( "IMGD".getBytes("UTF-8"));
		long filePointer = ra.getFilePointer();
		ra.writeInt(0);

		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		for( Part part : imageData) {
			ImageIO.write( part.getData(), "png", bos);
			System.out.println(bos.size());
			ra.writeInt( bos.size());
			ra.write(bos.toByteArray());
			bos.reset();
		}
		
		long filePointer2 = ra.getFilePointer();
		ra.seek(filePointer);
		
		if( filePointer2 - filePointer > Integer.MAX_VALUE )
			MDebug.handleError( ErrorType.OUT_OF_BOUNDS, null, "Image Data Too Big (>2GB).");
		ra.writeInt( (int)(filePointer2 - filePointer));
		
		ra.seek(filePointer2);
	}
}
