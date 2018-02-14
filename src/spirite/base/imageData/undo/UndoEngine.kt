package spirite.base.imageData.undo

import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MediumHandle

interface IUndoEngine {
    /** Cleans the slate, removing all undoable actions and reinitializing the base contexts*/
    fun reset()

    /** Current position of the Undo Queue (setting it will undo/redo actions to get to the position).
     *
     * 0 = base image (or last time reset was called), 1 = One Action Off, 2 = Two Actions Off, etc.*/
    var queuePosition: Int

    /** A list of all MediumData which is needed by the Undo Engine. */
    val dataUsed : Set<MediumHandle>


    /** Marks the current undo spot as the spot which is "unchanged" form the save*/
    fun setSaveSpot()
    val isAtSaveSpot: Boolean

    val undoHistory: List<UndoIndex>

    fun performAndStore( action: UndoableAction)
    fun doAsAggregateAction( runner: () -> Unit, description: String)

    fun undo()
    fun redo()
    fun cleanup()
}

class UndoIndex(
        val handle: MediumHandle?,
        val action: UndoableAction)


class UndoEngine(
        val workspace: IImageWorkspace
) : IUndoEngine {
    private val contexts = mutableListOf<UndoContext>()
    private val undoQueue = mutableListOf<UndoContext>()
    private var _queuePosition = 0
    private var saveSpot = 0

    override fun reset() {
        contexts.forEach { it.flush() }
        undoQueue.clear()
        _queuePosition = 0
        contexts.clear()
        contexts.add( NullContext())
        contexts.add( CompositeContext())
        saveSpot = 0
    }

    override var queuePosition: Int
        get() = TODO("not implemented")
        set(value) {}
    override val dataUsed: Set<MediumHandle> get() {
        val set = mutableSetOf<MediumHandle>()

        contexts.forEach {set.addAll( it.getImageDependencies())}

        return set
    }

    override fun setSaveSpot() { saveSpot = _queuePosition }
    override val isAtSaveSpot: Boolean = (saveSpot == _queuePosition)

    override val undoHistory: List<UndoIndex> get() {
        val contextIterators = contexts.map { Pair(it, it.iterator()) }.toMap()

        // Note hidden complexity: iterating multiple iterators within iterator
        return undoQueue.map { UndoIndex(it.medium, contextIterators[it]!!.next() )}
    }

    override fun performAndStore(action: UndoableAction) {
        if( action is ImageAction)
            contexts.add( ImageContext( action.building.handle))

        when {
            activeStoreState != null -> activeStoreState!!.add(action)
            else -> {
                action.performAction()
                storeAction( action)
            }
        }

        // TODO: I don't like this being here
        if( action is ImageAction)
            action.building.handle.refresh()
    }

    private fun storeAction( action: UndoableAction) {
        // Delete all actions stored after the current iterator point
        if( _queuePosition < undoQueue.size-1) {
            undoQueue.dropLast(undoQueue.size-1-_queuePosition)
            contexts.forEach { it.clipHead() }

            if( undoQueue.size <= saveSpot)
                saveSpot = -1
        }

        // If the UndoAction is a StackableAction and a compatible entry
        //   is on the top of the stack, modify that entry instead of creating
        //   a new one.
        if( undoQueue.size != 0 && _queuePosition == undoQueue.size-1) {
            // TODO
        }

    }

    // region Aggregate Action
    val storeStack = mutableListOf<MutableList<UndoableAction>>()
    var activeStoreState : MutableList<UndoableAction>? = null

    override fun doAsAggregateAction(runner: () -> Unit, description: String) {
        val storeState = mutableListOf<UndoableAction>()
        storeStack.add( storeState)
        activeStoreState = storeState

        runner.invoke()
        storeState.removeAt( storeStack.lastIndex)

        if( storeStack.isEmpty()) {
            val topList = storeStack.last()
            topList.addAll( storeState)
            activeStoreState = topList
        }
        else {
            activeStoreState = null
            performAndStore( CompositeAction(storeState, description))
        }
    }
    // endregion

    override fun undo() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun redo() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun cleanup() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}