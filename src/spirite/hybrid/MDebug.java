package spirite.hybrid;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

/***
 * 
 * @author Rory Burks
 *
 */
public class MDebug {
	
	//public static final boolean DEBUG = true;
	
	private static List<String> debugLog = new ArrayList<>();
	
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
		// A major structural error will probably cripple an entire aspect of
		//	the program.  They are huge problems that shouldn't happen and probably 
		// 	cannot be fixed without restarting the program but they aren't 
		//	immediately fatal.
		STRUCTURAL_MAJOR,
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

	public static void handleWarning(WarningType priority, String message) {
		System.out.println( "Warning: " + message);
		pushLog("Warning: " + message);
	}
	public static void handleWarning( WarningType priority, Object source, String message) {
		System.out.println( "Warning: " + message);
		pushLog("Warning: " + message);
	}

	public static void handleError( ErrorType type, String message) {
		Thread.dumpStack();
        JOptionPane.showMessageDialog(null, "Error: " + message);
        pushLog("Error: " + message);
	}
	public static void handleError( ErrorType type, Exception source, String message) {
		source.printStackTrace();
        JOptionPane.showMessageDialog(null, "Error: " + message);
        pushLog("Error: " + message);
	}

    public static void note( String message) {
        JOptionPane.showMessageDialog(null, message);
        pushLog("Note: " + message);
    }

    public static void log( String message) {
		System.out.println( "Log: " + message);
		pushLog("Log: " + message);
    }
    
    public static void clearDebugLog() {
    	debugLog.clear();
    	triggerLog();
    }
    
    private static void pushLog(String str) {
    	debugLog.add(str);
    	triggerLog();
    }
    public static List<String> getLog() {
    	return new ArrayList<>(debugLog);
    }
    
    
    // ==================
    // ==== Debug Observer
    public static interface DebugObserver {
    	public void logChanged();
    }
    private static final List<WeakReference<DebugObserver>> observers = new ArrayList<>();
    public static void addLogObserver(DebugObserver obs) {
    	observers.add(new WeakReference<MDebug.DebugObserver>(obs));
    }
    public static void removeLogObserver(DebugObserver obs) {
    	Iterator<WeakReference<DebugObserver>> it = observers.iterator();
    	while( it.hasNext()) {
    		DebugObserver other = it.next().get();
    		if( other == null || other.equals(obs))
    			it.remove();
    	}
    }
    private static void triggerLog(){
    	Iterator<WeakReference<DebugObserver>> it = observers.iterator();
    	while( it.hasNext()) {
    		DebugObserver obs = it.next().get();
    		if( obs == null) it.remove();
    		else
    			obs.logChanged();
    	}
    }

}
