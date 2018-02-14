package spirite.base.imageData.undo

import spirite.base.imageData.MediumHandle
import spirite.debug.SpiriteException
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType

/**
 * An UndoContext is at its heart simple a Storage Structure for UndoActions, however their iterative behavior can
 * differ wildly depending on implementation
 * */
interface UndoContext<T> : Iterable<UndoableAction> where T : UndoableAction {
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

}

class NullContext : UndoContext<NullAction> {
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

    override val medium: MediumHandle? = null

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

    override fun iterator() = actions.iterator()
}

class CompositeContext : UndoContext<CompositeAction> {
    override val medium: MediumHandle?
        get() = TODO("not implemented")

    override fun addAction(action: CompositeAction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun undo() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun redo() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clipHead() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clipTail() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEmpty(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun flush() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getImageDependencies(): Set<MediumHandle> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun iterator(): Iterator<UndoableAction> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

/**
 * Because actions on image data destructively covers a large amount of data, instead of either storing every individual
 * change as an image or painstakingly calculating the delta between two images, instead only every X (default 10)
 * changes are stored as images and the in-between frames are calculated working forward from that frame.
 */
class ImageContext(override val medium: MediumHandle) : UndoContext<ImageAction> {
    override fun addAction(action: ImageAction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun undo() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun redo() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clipHead() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clipTail() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEmpty(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun flush() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getImageDependencies(): Set<MediumHandle> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun iterator(): Iterator<UndoableAction> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}