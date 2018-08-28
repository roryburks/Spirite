package spirite.base.imageData.undo

import spirite.base.brains.IObservable
import spirite.base.brains.Observable
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.undo.IUndoEngine.UndoHistoryChangeEvent
import spirite.base.util.groupExtensions.SinglyList
import spirite.base.util.groupExtensions.then

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
    fun doAsAggregateAction( description: String, stackable: Boolean = false, runner: () -> Unit)

    fun undo() : Boolean
    fun redo() : Boolean
    fun cleanup()

    class UndoHistoryChangeEvent(
            val history: List<UndoIndex>?,  // Null if not a History Change Event
            val undoEngine: IUndoEngine,
            val position: UndoIndex)
    val undoHistoryObserver : IObservable<(UndoHistoryChangeEvent)->Any?>
}

class UndoIndex(
        val handle: MediumHandle?,
        val action: UndoableAction)


class UndoEngine(
        val workspace: MImageWorkspace,
        val mediumRepo: MMediumRepository
) : IUndoEngine {

    private var nullContext : NullContext = NullContext()
    private val imageContexts = mutableListOf<ImageContext>()
    private var compositeContext: CompositeContext = CompositeContext(nullContext, imageContexts, mediumRepo)
    private val contexts get()
            = SinglyList(nullContext).then(SinglyList(compositeContext)).then(imageContexts)

    private val undoQueue = mutableListOf<UndoContext<*>>()
    private var _queuePosition = 0
    private var saveSpot = 0


    override fun reset() {
        contexts.forEach { it.flush() }
        undoQueue.clear()
        _queuePosition = 0
        imageContexts.clear()
        saveSpot = 0
        nullContext = NullContext()
        compositeContext = CompositeContext(nullContext, imageContexts, mediumRepo)
    }

    override var queuePosition: Int
        get() = _queuePosition
        set(value) {}
    override val dataUsed: Set<MediumHandle> get() {
        val set = mutableSetOf<MediumHandle>()

        contexts.forEach {set.addAll( it.getImageDependencies())}

        return set
    }

    override fun setSaveSpot() { saveSpot = _queuePosition }
    override val isAtSaveSpot: Boolean = (saveSpot == _queuePosition)

    override val undoHistory: List<UndoIndex> get() {
        // Note: Hidden complexity: iterating multiple iterators within iterator
        contexts.forEach { it.startIteration() }
        return undoQueue.map {UndoIndex(it.medium, it.iterateNext() )}
    }

    // region Core Functionality
    override fun performAndStore(action: UndoableAction) {
        if( action is ImageAction && !imageContexts.any{it.medium == action.arranged.handle})
            imageContexts.add(ImageContext(action.arranged.handle, mediumRepo))

        action.performAction()
        activeStoreState?.add(action) ?: storeAction(action)

        // TODO: I don't like this being here
        if( action is ImageAction)
            action.arranged.handle.refresh()
    }

    private fun storeAction( action: UndoableAction) {
        // Delete all actions stored after the current iterator point
        if( _queuePosition < undoQueue.size) {
            undoQueue.subList(_queuePosition,undoQueue.size).clear()
            contexts.forEach { it.clipHead() }

            if( undoQueue.size <= saveSpot)
                saveSpot = -1
        }

        // If the UndoAction is a StackableAction and a compatible entry
        //   is on the top of the stack, modify that entry instead of creating
        //   a new one.
        if( undoQueue.size != 0 && _queuePosition == undoQueue.size) {
            val lastAction = undoQueue[_queuePosition-1].lastAction
            if( lastAction is StackableAction && lastAction.canStack(action)) {
                lastAction.stackNewAction(action)
                return
            }
        }

        // Determine the workspace for the given Action and add it
        val context : UndoContext<*> = when( action) {
            is NullAction -> {
                nullContext.addAction( action)
                nullContext
            }
            is CompositeAction -> {
                compositeContext.addAction(action)
                compositeContext
            }
            is ImageAction -> {
                var imageContext = imageContexts.find { it.medium == action.arranged.handle }

                if( imageContext == null) {
                    imageContext = ImageContext(action.arranged.handle,mediumRepo)
                    imageContexts.add(imageContext)
                }
                imageContext.addAction(action)
                imageContext
            }
        }
        action.onAdd()

        undoQueue.add(context)
        _queuePosition++
        triggerHistoryChanged()
        cull()
        workspace.hasChanged = true
    }

    override fun undo() : Boolean {
        if( _queuePosition == 0)
            return false
        else {
            --_queuePosition
            undoQueue[_queuePosition].undo()
            triggerUndo()
            workspace.hasChanged = _queuePosition != saveSpot
            return true
        }
    }

    override fun redo() : Boolean {
        if( _queuePosition == undoQueue.size)
            return false
        else {
            undoQueue[_queuePosition].redo()
            ++_queuePosition
            workspace.hasChanged = _queuePosition != saveSpot
            return true
        }
    }
    // endregion

    // region Aggregate Action
    private val storeStack = mutableListOf<MutableList<UndoableAction>>()
    private var activeStoreState : MutableList<UndoableAction>? = null

    override fun doAsAggregateAction( description: String, stackable: Boolean, runner: () -> Unit) {
        val storeState = mutableListOf<UndoableAction>()
        storeStack.add( storeState)
        activeStoreState = storeState

        runner.invoke()
        storeStack.removeAt( storeStack.lastIndex)

        if( !storeStack.isEmpty()) {
            val topList = storeStack.last()
            topList.addAll( storeState)
            activeStoreState = topList
        }
        else {
            activeStoreState = null
            storeAction( when( stackable) {
                false -> CompositeAction(storeState, description)
                true -> StackableCompositeAction(storeState, description)
            })
        }
    }
    // endregion

    private fun cull() {
        // TODO
    }


    override fun cleanup() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun triggerHistoryChanged() {
        undoHistoryObserver.trigger { it.invoke(UndoHistoryChangeEvent(undoHistory, this, undoHistory[_queuePosition-1]))}
    }
    private fun triggerUndo() {
        undoHistoryObserver.trigger { it.invoke(UndoHistoryChangeEvent(null, this, undoHistory[_queuePosition-1]))}
    }

    override val undoHistoryObserver = Observable<(UndoHistoryChangeEvent)->Any?>()
}