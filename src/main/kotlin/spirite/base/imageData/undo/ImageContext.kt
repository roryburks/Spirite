package spirite.base.imageData.undo

import rb.extendo.dataStructures.SinglySet
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.imageData.mediums.DynamicMedium
import spirite.core.hybrid.DebugProvider
import spirite.core.hybrid.IDebug
import spirite.core.hybrid.IDebug.ErrorType.STRUCTURAL
import spirite.core.hybrid.IDebug.ErrorType.STRUCTURAL_MINOR
import spirite.pc.TestConfig

val MAX_TICKS_PER_KEY = 10

/**
 * Because actions on image data destructively covers a large amount of data, instead of either storing every individual
 * change as an image or painstakingly calculating the delta between two images, instead only every AnimationCommand (default 10)
 * changes are stored as images and the in-between frames are calculated working forward from that frame.
 */
class ImageContext
constructor(
        override val medium: MediumHandle,
        private val workspace: MImageWorkspace,
        private val _debug : IDebug = DebugProvider.debug) : UndoContext<ImageAction>
{
    private val actions = mutableListOf<ImageAction>()
    private var pointer = 0     // The poisition on the actionsList
    private var met = 0         // Amount of actions it's been since a keyframe
    private var vstart = 0      // The first "valid" action.  As the tail is clipped,
                                // this increments until it hits a Keyframe, then it removes
                                // the old base keyframe and adjusts

    override val lastAction: ImageAction get() = actions.last()

    val size get() = actions.size - 1 - vstart
    val effectivePointer get() = pointer - vstart

    init {
        actions.add(KeyframeAction(null))
    }

    override fun addAction(action: ImageAction) {
        met++
        pointer++

        if( met == MAX_TICKS_PER_KEY || action.isHeavy) {
            actions.add( KeyframeAction(action))
            met = 0
        }
        else {
            if( action is KeyframeAction)
                met = 0
            actions.add(action)
        }
    }

    override fun undo() {
        // Undo the logical action
        actions[pointer].undoAction()

        --pointer
        --met
        if( pointer < 0 )
            _debug.handleError(STRUCTURAL, "Internal Undo attempted before start of workspace.")

        // Find the previous KeyframeAction
        if( met < 0) {
            var i=0
            while( i < MAX_TICKS_PER_KEY) {
                if( actions[pointer-i] is KeyframeAction)
                    break
                ++i
            }
            met = i
        }

        // Refresh the Image to the current most recent keyframe
        actions[pointer-met].performImageAction(actions[pointer-met].arranged.built)

        for( index in (pointer-met+1)..pointer) {
            actions[index].performImageAction(actions[index].arranged.built)
        }
    }

    override fun redo() {
        pointer++
        met++
        if( pointer >= actions.size || pointer == 0) {
            _debug.handleError(STRUCTURAL, "Undo Outer queue desynced with inner queue.")
            return
        }
        if(actions[pointer] is KeyframeAction)
            met = 0
        actions[pointer].performAction()
    }

    override fun clipHead() {
        val sublist = actions.subList( pointer+1,actions.size)
        sublist.forEach { it.onDispatch() }
        sublist.clear()
    }

    override fun clipTail() {
        if( vstart == pointer)
            _debug.handleError(STRUCTURAL_MINOR, "Tried to clip more than exists in ImageContext")

        vstart++
        if( actions[vstart] is KeyframeAction) {
            val sublist = actions.subList(0,vstart)
            pointer -= sublist.size
            sublist.forEach { it.onDispatch() }
            sublist.clear()
            vstart = 0
        }
    }

    override fun isEmpty() = (actions.size <= vstart + 1)

    override fun flush() {
        actions.forEach { it.onDispatch() }
    }

    override fun getImageDependencies(): Set<MediumHandle> = SinglySet(medium)

    // region Iteration
    private var iterMet = 0
    override fun startIteration() {
        iterMet = 1 // 1 to skip over the initial keyframe
    }

    override fun iterateNext(): UndoableAction {
        val action = actions[iterMet++]
        return when( action) {
            is KeyframeAction -> action.underlyingAction ?: action
            else -> action
        }
    }
    // endregion

    /** A KeyframeAction is a special kind of ImageAction which instead of
     * storing the way the image was changed, but instead stores the
     * entire image as it should appear after the action.  It stores the
     * action that it's "supposed" to be in hiddenAction and performs
     * its logical components (if it has any).*/
    private inner class KeyframeAction(
            val underlyingAction: ImageAction?
    ) : ImageAction(ArrangedMediumData(medium, 0f, 0f)){
        override val description: String get() = "Keyframe Action (you probably shouldn't be able to see this)"

        override fun undoAction() {
            underlyingAction?.undoAction()
        }

        override fun performImageAction(built: BuiltMediumData) {

            (frame as? DynamicMedium)?.image?.base?.apply { TestConfig.trySave(this,"1.png") }
            val duped = frame.dupe(workspace)
            (duped as? DynamicMedium)?.image?.base?.apply { TestConfig.trySave(this,"2.png") }
            workspace.mediumRepository.replaceMediumDirect( medium, duped)
            medium.refresh()
        }

        private val frame = medium.medium.dupe(workspace)

        override fun onAdd() {
            underlyingAction?.onAdd()
        }
        override fun onDispatch() {
            // Most of the times, this should be the only thing that has
            //	a medium on the CachedImage, but in special cases, the
            //	cached image might have been passed to it, so a multi-user
            //	scheme is relevant.
            frame.flush()
            underlyingAction?.onDispatch()
        }
    }
}