package spirite.base.file;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

/***
 * A container for various Utility functions and constants for files IO
 * 
 * @author Rory Burks
 *
 */
public class SaveLoadUtil {
	public static class AAF {
		public static final int version = 1;
	}
	
	// :::: Generic Identifier
	public static final byte UNKNOWN = (byte)0xFF;
	// :::: Node Type Identifiers for the SIFF GroupTree Section
	public static final byte NODE_GROUP = 0x00;
	public static final byte NODE_SIMPLE_LAYER = 0x01;
	public static final byte NODE_RIG_LAYER = 0x02;	
	// :::: Animation Type Identifiers
	public static final byte ANIM_FIXED_FRAME = 0x01;
	
	
	
	public static final int VERSION = 0x0000_0004;

	// Node Attribute Masks
	public static final int VISIBLE_MASK = 0x01;
	public static final int EXPANDED_MASK = 0x02;
	
	// Image Attribute Masks
	public static final int DYNMIC_MASK = 0x01;
	
	/***
	 * Gets the current SIFF header
	 */
	public static byte[] getHeader() 
			throws UnsupportedEncodingException
	{
		byte b[] = new byte[4];
		System.arraycopy( "SIFF".getBytes("UTF-8"), 0, b, 0, 4);
		return b;
	}
	
	/***
	 * Converts a string to a Null-Terminated byte array using
	 * UTF-8 charset
	 */
	public static byte[] strToByteArrayUTF8( String str) 
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
}
