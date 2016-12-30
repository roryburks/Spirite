package spirite;

import javax.swing.JOptionPane;

/***
 * 
 * @author Rory Burks
 *
 */
public class MDebug {
	
	public static enum WarningType {
		// A structural warning is an issue that should never happen according to 
		//	program design.  It indicates some potentially major oversight, but
		//	because it should not significantly effect the performance of the
		//	program it is just a warning.
		STRUCTURAL,		
		UNSPECIFIED
	}
	
	public static void handleWarning( WarningType priority, Object source, String message) {
		System.out.println( "Warning: " + message);
	}
	
    public static void handleError( int priority, String message) {
        JOptionPane.showMessageDialog(null, "MDB: " + message);
    }

    public static void handleError( int priority, String message, Exception e) {
        JOptionPane.showMessageDialog(null, "MDB: " + message);
    }

    public static void handleError( int priority, Exception e) {
        JOptionPane.showMessageDialog(null, "MDB: " + e.getCause() + "/n" + e.getMessage() + "/n" + e.toString());
    }

    public static void note( String message) {
        JOptionPane.showMessageDialog(null, message);
    }
}
