package spirite.base.imageData.undo

import spirite.base.imageData.BuildingMediumData
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.mediums.BuiltMediumData


sealed class  UndoableAction {
    abstract val description : String
    abstract fun performAction()
    abstract fun undoAction()
    fun onDispatch() {}
    fun onAdd() {}
}

abstract class ImageAction(
        val building: BuildingMediumData
) : UndoableAction() {
    override fun performAction() {
        performNonimageAction()
        building.doOnBuildData { performImageAction( it) }
    }

    open fun performNonimageAction() {}
    abstract fun performImageAction( built: BuiltMediumData)
}

/**
 * In contrast to image actions, most non-image actions are
 */
abstract class NullAction() : UndoableAction() {
    open fun getDependencies(): Collection<MediumHandle>? = null
}

class CompositeAction(
        actions: List<UndoableAction>,
        override val description: String
) : UndoableAction() {
    val actions = actions.toTypedArray()

    override fun performAction() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun undoAction() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}