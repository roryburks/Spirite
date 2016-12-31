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
		// An initialization error means that some component (probably a dynamically
		//	created one) could not be created for some reason.  This shouldn't
		//	interfere with the operation of the program, but may prevent a piece of
		//	it from being created.
		INITIALIZATION,
		// From action commands to properties to globals, there are a great many
		//	thinks that are referenced by strings along the lines of "global.new_image"
		//	Reference warnings are failures to find the corresponding data
		REFERENCE,
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
