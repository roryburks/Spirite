package spirite;

import javax.swing.JOptionPane;

/***
 * 
 * @author Rory Burks
 *
 */
public class MDebug {
	
	public static final boolean DEBUG = true;
	
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
		// Most likely due to out-of-memory error or some system lock.
		ALLOCATION_FAILED,
		// Indicates that a core resource (such as an icon sheet) is not loading
		//	properly.  While the program can probably function without it, it will
		//	not be very user-friendly.
		RESOURCE,
		// A Fatal Error can and should immediately terminate the program
		FATAL,
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
		// Called when the program tries to access data that has been locked
		LOCK_CONFLICT,
		UNSPECIFIED
	}
	
	public static void handleWarning( WarningType priority, Object source, String message) {
		System.out.println( "Warning: " + message);
	}
	
	public static void handleError( ErrorType type, Object source, String message) {
		Thread.dumpStack();
        JOptionPane.showMessageDialog(null, "Error: " + message);
	}

    public static void note( String message) {
        JOptionPane.showMessageDialog(null, message);
    }
}
