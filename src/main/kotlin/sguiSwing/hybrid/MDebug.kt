package sguiSwing.hybrid

import rb.owl.IObservable
import rb.owl.Observable
import javax.swing.JOptionPane

object MDebug {
    enum class ErrorType {
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
        GL
    }

    enum class WarningType {
        // A structural warning is an issue that should never happen according to
        //	program design.  It indicates some potentially major oversight, but
        //	because it should not significantly effect the performance of the
        //	program it is just a warning.
        STRUCTURAL,
        // An initialization error means that some resizeComponent (probably a dynamically
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

    fun handleWarning( priority: WarningType, message: String, origin: Exception? = null) {
        Hybrid.beep()
        println("Warning: $message")
        pushLog("Warning: $message")
    }

    fun handleError( type: ErrorType, message: String, origin: Exception? = null) {
        origin?.printStackTrace() ?: Thread.dumpStack()
        JOptionPane.showMessageDialog( null, "Error: $message")
        pushLog("Error: $message")
    }

    fun note( str: String) = pushLog("Note: $str")
    fun log( str: String) = pushLog( "Log: $str")

    fun clearDebugLog() {
        _debugLog.clear()
        trigger()
    }

    val debugLog : List<String> get() = _debugLog
    private val _debugLog = mutableListOf<String>()
    private fun pushLog( str: String) {
        _debugLog.add(str)
        trigger()
    }

    interface  DebugObserver {
        fun logChanged()
    }
    val debugObservable: IObservable<DebugObserver> get() = _debugObservable
    private val _debugObservable = Observable<DebugObserver>()
    private fun trigger() = _debugObservable.trigger { it.logChanged() }
}