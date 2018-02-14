package spirite.base.imageData.undo

import spirite.base.imageData.BuildingMediumData
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.mediums.BuiltMediumData
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType
import spirite.hybrid.MDebug.ErrorType.STRUCTURAL_MINOR

/**
 * An UndoContext is at its heart simple a Storage Structure for UndoActions, however their iterative behavior can
 * differ wildly depending on implementation
 * */
interface UndoContext<T> where T : UndoableAction {
    val medium: MediumHandle?

    fun addAction( action: T)
    fun undo()
    fun redo()

    /** Removes All points after the current pointer */
    fun clipHead()

    /** Removes the oldest entry */
    fun clipTail()

    fun isEmpty() : Boolean
    fun flush()
    fun getImageDependencies() : Set<MediumHandle>

    /** Iteration is synchronized in that CompositeAction increments these manually, so it doesn't make sense to pass
     * out the iterator.  If multiple concurrent iterations are needed in the future, can make StartIteration pass out
     * a reference that is passed in to iterateNext. */
    fun startIteration()
    fun iterateNext() : UndoableAction
}

class NullContext : UndoContext<NullAction> {
    override val medium: MediumHandle? = null
    private val actions = mutableListOf<NullAction>()
    private var pointer = 0

    override fun addAction(action: NullAction) {
        actions.add(action)
        pointer = actions.size
    }

    override fun undo() {
        if( pointer == 0) MDebug.handleError(ErrorType.STRUCTURAL_MINOR, "Undo Queue Desync: tried to undo Null Context before beginning")
        else {
            pointer--
            actions[pointer].undoAction()
        }
    }

    override fun redo() {
        if( pointer == actions.size)MDebug.handleError(ErrorType.STRUCTURAL_MINOR, "Undo Queue Desync: tried to redo Null Context after end")
        else {
            actions[pointer].performAction()
            pointer++
        }
    }

    override fun clipHead() {
        actions.subList(pointer, actions.size).clear()
    }
    override fun clipTail() {
        actions[0].onDispatch()
        actions.removeAt(0)
    }

    override fun isEmpty() = actions.isEmpty()

    override fun getImageDependencies(): Set<MediumHandle> {
        val set = mutableSetOf<MediumHandle>()
        actions.forEach {
            val dep = it.getDependencies()
            if( dep != null) set.addAll( dep)
        }
        return set
    }

    override fun flush() {
        actions.forEach { it.onDispatch() }
    }

    // region Iteration
    private var met = 0
    override fun startIteration() {
        met = 0
    }

    override fun iterateNext() = actions[met++]
    // endregion

}

class CompositeContext(
        private val nullContext: NullContext,
        private val imageContexts: MutableList<ImageContext>
) : UndoContext<CompositeAction> {

    override val medium: MediumHandle? = null
    private val actions = mutableListOf<CompositeAction>()
    private var pointer = 0

    override fun addAction(action: CompositeAction) {
        action.actions.forEach { innerAction ->
            when( innerAction ) {
                is NullAction -> nullContext.addAction(innerAction)
                is ImageAction -> {
                    var imageContext = imageContexts.find { it.medium == innerAction.building.handle }

                    if( imageContext == null) {
                        imageContext = ImageContext(innerAction.building.handle)
                        imageContexts.add(imageContext)
                    }
                }
                else -> MDebug.handleError(STRUCTURAL_MINOR, "Other type got mixed into compositeAction: ${innerAction::class}")
            }
        }
        actions.add(action)
        pointer = actions.size
    }

    // region Duplicate Code From NullContext
    override fun undo() {
        if( pointer == 0) MDebug.handleError(ErrorType.STRUCTURAL_MINOR, "Undo Queue Desync: tried to undo Null Context before beginning")
        else {
            pointer--
            actions[pointer].undoAction()
        }
    }

    override fun redo() {
        if( pointer == actions.size)MDebug.handleError(ErrorType.STRUCTURAL_MINOR, "Undo Queue Desync: tried to redo Null Context after end")
        else {
            actions[pointer].performAction()
            pointer++
        }
    }

    override fun clipHead() {
        actions.subList(pointer, actions.size).clear()
    }

    override fun clipTail() {
        actions[0].onDispatch()
        actions.removeAt(0)
    }

    override fun isEmpty() = actions.isEmpty()
    // endregion

    override fun flush() {
        // shouldn't need to flush as it's handled by the other contexts
    }

    override fun getImageDependencies(): Set<MediumHandle> {
        val set = mutableSetOf<MediumHandle>()
        // Handled by other contexts
        return set
    }


    // region Iteration
    var met = 0
    override fun startIteration() {
        met = 0
    }

    override fun iterateNext(): UndoableAction {
        val composite = actions[met++]
        composite.actions.forEach { action ->
            when( action) {
                is NullAction -> nullContext.iterateNext()
                is ImageAction -> imageContexts.find { action.building.handle == it.medium }?.iterateNext()
                else -> MDebug.handleError(STRUCTURAL_MINOR, "Other type got mixed into compositeAction: ${action::class}")
            }
        }
        return composite
    }
    // endregion
}


val MAX_TICKS_PER_KEY = 10

/**
 * Because actions on image data destructively covers a large amount of data, instead of either storing every individual
 * change as an image or painstakingly calculating the delta between two images, instead only every X (default 10)
 * changes are stored as images and the in-between frames are calculated working forward from that frame.
 */
class ImageContext(override val medium: MediumHandle) : UndoContext<ImageAction> {
    private val workspace get() = medium.context
    private val actions = mutableListOf<ImageAction>()
    private var pointer = 0 // The poisition on the actionsList
    private var met = 0     // Amount of actions it's been since a keyframe
    private var vstart = 0  // The first "valid" action.  As the tail is clipped,
                            // this increments until it hits a Keyframe, then it removes
                            // the old base keyframe and adjusts

    init {
        actions.add(KeyframeAction(null))
    }

    override fun addAction(action: ImageAction) {
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
            MDebug.handleError(ErrorType.STRUCTURAL, "Internal Undo attempted before start of context.")

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

        for( index in (pointer-met+1) until pointer) {
            actions[index].building.doOnBuildData { actions[index].performImageAction(it) }
        }

        // Construct ImageChangeEvent and send it
        // TODO
    }

    override fun redo() {
        pointer++
        met++
        if( pointer >= actions.size || pointer == 0) {
            MDebug.handleError(ErrorType.STRUCTURAL, "Undo Outer queue desynced with inner queue.")
            return
        }
        if(actions[pointer] is KeyframeAction)
            met = 0
        for( index in (pointer-met) until pointer) {
            actions[index].performAction()
        }

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
            MDebug.handleError(ErrorType.STRUCTURAL_MINOR, "Tried to clip more than exists in ImageContext")

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

    override fun getImageDependencies(): Set<MediumHandle> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
    ) : ImageAction(BuildingMediumData(medium,0f,0f)){
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
            //	a handle on the CachedImage, but in special cases, the
            //	cached image might have been passed to it, so a multi-user
            //	scheme is relevant.
            frame.flush()
            underlyingAction?.onDispatch()
        }
    }
}