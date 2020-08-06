package spirite.base.imageData.undo

import spirite.base.imageData.MediumHandle
import sguiSwing.hybrid.MDebug
import sguiSwing.hybrid.MDebug.ErrorType.STRUCTURAL_MINOR

/***
 * NullContext is a special workspace that is not associated with any ImageData
 * As such the concept of keyframes to work from doesn't make sense.  Instead
 * Each task is stored.  But since you are not strictly working forwards, the
 * UndoActions associated with NullContext must have two-way methods for
 * performing AND undoing.
 *
 * Since it's just a straight queue storing logical data, its behavior is simple
 * compared to ImageContext.
 */
class NullContext : UndoContext<NullAction> {
    override val medium: MediumHandle? = null
    private val actions = mutableListOf<NullAction>()
    var pointer = 0 ; private set
    val size get() = actions.size

    override val lastAction: NullAction get() = actions.last()

    override fun addAction(action: NullAction) {
        actions.add(action)
        pointer = actions.size
    }

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
        val sublist = actions.subList(pointer, actions.size)
        sublist.forEach { it.onDispatch() }
        sublist.clear()
    }
    override fun clipTail() {
        actions[0].onDispatch()
        actions.removeAt(0)
        pointer--
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