package spirite.base.imageData.undo

import spirite.base.imageData.MediumHandle
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.BuiltMediumData


sealed class  UndoableAction {
    abstract val description : String
    abstract fun performAction()
    abstract fun undoAction()
    open fun onDispatch() {}
    open fun onAdd() {}
}

abstract class ImageAction(
        val arranged: ArrangedMediumData
) : UndoableAction() {
    override fun performAction() {
        performNonimageAction()
        performImageAction(arranged.built)
    }

    override fun undoAction() {}    // Can have an logical undoAction associated with it (shouldn't effect the image, though)
    open fun performNonimageAction() {}
    abstract fun performImageAction( built: BuiltMediumData)
    open val isHeavy = false
}

/**
 * In contrast to image actions, most non-image actions are
 */
abstract class NullAction() : UndoableAction() {
    open fun getDependencies(): Collection<MediumHandle>? = null
}

class CompositeAction(
        actions: Iterable<UndoableAction>,
        override val description: String
) : UndoableAction() {

    internal val actions : List<UndoableAction>
    init {
        // Clear all CompositeActions from the list and replace them with
        //	their list of action.
        // Note: No need for recursive checking as CompositeAction is closed to inheritance.
        val sanitized = mutableListOf<UndoableAction>()

        actions.forEach { action ->
            when( action) {
                is CompositeAction -> action.actions.forEach { sanitized.add(it)}
                else -> sanitized.add(action)
            }
        }

        this.actions = sanitized
    }

    override fun performAction() {
        actions.forEach { it.performAction() }
    }

    override fun undoAction() {
        actions.asReversed().forEach { it.undoAction() }
    }
}