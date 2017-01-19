package spirite.file;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

/***
 * A container for various Utility functions for files IO
 * 
 * 
 * 
 * @author Rory Burks
 *
 */
public class SaveLoadUtil {
	// :::: Node Type Identifiers for the SIFF GroupTree Section
	public static final byte NODE_GROUP = 0x00;
	public static final byte NODE_SIMPLE_LAYER = 0x01;
	public static final byte NODE_UNKNOWN = (byte)0xFF;
	
	/***
	 * Gets the current SIFF header
	 */
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
