package spirite.base.imageData.undo

import spirite.base.imageData.mediums.BuildingMediumData
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.util.groupExtensions.SinglySet
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.STRUCTURAL
import spirite.hybrid.MDebug.ErrorType.STRUCTURAL_MINOR

val MAX_TICKS_PER_KEY = 10

/**
 * Because actions on image data destructively covers a large amount of data, instead of either storing every individual
 * change as an image or painstakingly calculating the delta between two images, instead only every X (default 10)
 * changes are stored as images and the in-between frames are calculated working forward from that frame.
 */
class ImageContext(override val medium: MediumHandle) : UndoContext<ImageAction> {
    private val workspace get() = medium.context
    private val actions = mutableListOf<ImageAction>()
    private var pointer = 0     // The poisition on the actionsList
    private var met = 0         // Amount of actions it's been since a keyframe
    private var vstart = 0      // The first "valid" action.  As the tail is clipped,
                                // this increments until it hits a Keyframe, then it removes
                                // the old base keyframe and adjusts

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
            MDebug.handleError(STRUCTURAL, "Internal Undo attempted before start of context.")

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
        actions[pointer-met].building.doOnBuildData { actions[pointer-met].performImageAction(it) }

        for( index in (pointer-met+1)..pointer) {
            actions[index].building.doOnBuildData { actions[index].performImageAction(it) }
        }

        // Construct ImageChangeEvent and send it
        // TODO
    }

    override fun redo() {
        pointer++
        met++
        if( pointer >= actions.size || pointer == 0) {
            MDebug.handleError(STRUCTURAL, "Undo Outer queue desynced with inner queue.")
            return
        }
        if(actions[pointer] is KeyframeAction)
            met = 0
        actions[pointer].performAction()

        // Construct ImageChangeEvent and send it
        // TODO
    }

    override fun clipHead() {
        val sublist = actions.subList( pointer+1,actions.size)
        sublist.forEach { it.onDispatch() }
        sublist.clear()
    }

    override fun clipTail() {
        if( vstart == pointer)
            MDebug.handleError(STRUCTURAL_MINOR, "Tried to clip more than exists in ImageContext")

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

    override fun getImageDependencies(): Set<MediumHandle> {return SinglySet(medium)
    }

    // region Iteration
    private var iterMet = 0
    override fun startIteration() {
        iterMet = 1 // 1 to skip over the initial keyframe
    }

    override fun iterateNext(): UndoableAction {
        val action = actions[iterMet]
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
    ) : ImageAction(BuildingMediumData(medium, 0f, 0f)){
        override val description: String get() = "Keyframe Action (you probably shouldn't be able to see this)"

        override fun undoAction() {
            underlyingAction?.undoAction()
        }

        override fun performImageAction(built: BuiltMediumData) {
            workspace.mediumRepository.replaceMediumDirect( medium, frame.dupe())
        }

        private val frame = workspace.mediumRepository.getData(medium.id).dupe()

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