package spirite.file;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

public class SaveLoadUtil {
	public static final byte NODE_GROUP = 0x00;
	public static final byte NODE_LAYER = 0x01;
	
	
	/***
	 * Converts a string to a Null-Terminated byte array using
	 * UTF-8 charset
	 */
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
