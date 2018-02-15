package spirite.base.imageData.undo

import spirite.base.imageData.MediumHandle
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.STRUCTURAL_MINOR

/**
 * The CompositeContext is a special Context which can store multiple actions
 * (both Image and Null) in a single action.  A CompositeAction is performed
 * in order that they are added to the list and undone in reverse order.
 */
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
        if( pointer == 0) MDebug.handleError(STRUCTURAL_MINOR, "Undo Queue Desync: tried to undo Null Context before beginning")
        else {
            pointer--
            actions[pointer].undoAction()
        }
    }

    override fun redo() {
        if( pointer == actions.size) MDebug.handleError(STRUCTURAL_MINOR, "Undo Queue Desync: tried to redo Null Context after end")
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