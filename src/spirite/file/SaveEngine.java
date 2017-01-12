package spirite.file;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.imageio.ImageIO;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageData;
import spirite.image_data.ImageWorkspace;

/***
 * SaveEngine is a static container for methods which saves images to
 * various file formats (but particularly the native SIF format)
 */
public class SaveEngine {
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
	private static void _sgt_rec( GroupTree.Node node, RandomAccessFile ra, byte depth) 
			throws UnsupportedEncodingException, IOException 
	{
		if( node instanceof GroupTree.GroupNode) {
			GroupTree.GroupNode gnode = (GroupTree.GroupNode) node;
			
			// [1] : Depth of Node in GroupTree
			ra.write( depth);
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
			GroupTree.LayerNode rnode = (GroupTree.LayerNode) node;
			
			// [1] : Depth of Node in GroupTree
			ra.write( depth);
			// [1] : Node Type ID
			ra.write( SaveLoadUtil.NODE_LAYER);
			
			ImageData data = rnode.getImageData();
			// [4] : ID of ImageData linked to this LayerNode
			ra.writeInt( data.getID());
			
			// [n] : Null-terminated UTF-8 String for Layer name
			ra.write( SaveLoadUtil.strToByteArrayUTF8( rnode.getName()));
		}
		else {
			MDebug.handleWarning(WarningType.STRUCTURAL, null, "Unknown GroupTree Node type on saving.");
		}
	}
	
	private static void saveImageData( List<ImageData> imageData, RandomAccessFile ra) 
			throws UnsupportedEncodingException, IOException 
	{
		// [4] : "IMGD" tag
		ra.write( "IMGD".getBytes("UTF-8"));
		long filePointer = ra.getFilePointer();
		
		// [4] : Length of ImageData Chunk
		ra.writeInt(0);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		for( ImageData part : imageData) {
			// (Foreach ImageData)
			ImageIO.write( part.getData(), "png", bos);

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
}
