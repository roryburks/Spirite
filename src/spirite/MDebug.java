package spirite;

import javax.swing.JOptionPane;

/***
 * 
 * @author Rory Burks
 *
 */
public class MDebug {
	
	public static enum ErrorType {
		FILE,
		// Something being made that is either too big or too small
		//	or somehow gets data out of bounds
		OUT_OF_BOUNDS,
		// A minor structural error is more serious than a structural warning and
		//	indicates a potential substantial flaw in program design, but shouldn't
		//	cause a catastrophic failure of the program.
		STRUCTURAL_MINOR,
		STRUCTURAL,
	}
	
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
		// Called when the user or program tries to do something that it expects to
		//	be supported but isn't (probably because of version problems)
		UNSUPPORTED,
		UNSPECIFIED
	}
	
	public static void handleWarning( WarningType priority, Object source, String message) {
		System.out.println( "Warning: " + message);
	}
	
	public static void handleError( ErrorType type, Object source, String message) {

        JOptionPane.showMessageDialog(null, "Error: " + message);
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
