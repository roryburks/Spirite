package spirite.base.imageData.undo

import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MediumHandle
import sguiSwing.hybrid.MDebug
import sguiSwing.hybrid.MDebug.ErrorType.STRUCTURAL_MINOR

/**
 * The CompositeContext is a special Context which can store multiple actions
 * (both Image and Null) in a single action.  A CompositeAction is performed
 * in order that they are added to the list and undone in reverse order.
 */
class CompositeContext(
        private val nullContext: NullContext,
        private val imageContexts: MutableList<ImageContext>,
        private val workspace: MImageWorkspace
) : UndoContext<CompositeAction> {
    override val medium: MediumHandle? = null
    private val actions = mutableListOf<CompositeAction>()
    var pointer = 0 ; private set
    val size get() = actions.size


    override val lastAction: CompositeAction get() = actions.last()

    override fun addAction(action: CompositeAction) {
        val contexts = mutableListOf<UndoContext<*>>()
        action.actions.forEach { innerAction ->
            when( innerAction ) {
                is NullAction -> {
                    contexts.add(nullContext)
                    nullContext.addAction(innerAction)
                }
                is ImageAction -> {
                    var imageContext = imageContexts.find { it.medium == innerAction.arranged.handle }

                    if( imageContext == null) {
                        imageContext = ImageContext(innerAction.arranged.handle, workspace)
                        imageContexts.add(imageContext)
                    }

                    contexts.add(imageContext)
                    imageContext.addAction( innerAction)
                }
                else -> MDebug.handleError(STRUCTURAL_MINOR, "Other type got mixed into compositeAction: ${innerAction::class}")
            }
        }
        action.contexts = contexts
        actions.add(action)
        pointer = actions.size
    }

    // region Duplicate Code From NullContext
    override fun undo() {
        if( pointer == 0) MDebug.handleError(STRUCTURAL_MINOR, "Undo Queue Desync: tried to undo Null Context before beginning")
        else {
            pointer--
            actions[pointer].contexts.forEach { it.undo() }
        }
    }

    override fun redo() {
        if( pointer == actions.size) MDebug.handleError(STRUCTURAL_MINOR, "Undo Queue Desync: tried to redo Null Context after end")
        else {
            actions[pointer].contexts.forEach { it.redo() }
            pointer++
        }
    }

    override fun clipHead() {
        actions.subList(pointer, actions.size).clear()
    }

    override fun clipTail() {
        actions[0].onDispatch()
        actions.removeAt(0)
        pointer--
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
                is ImageAction -> imageContexts.find { action.arranged.handle == it.medium }?.iterateNext()
                else -> MDebug.handleError(STRUCTURAL_MINOR, "Other type got mixed into compositeAction: ${action::class}")
            }
        }
        return composite
    }
    // endregion
}