package spirite.base.imageData.undo

import spirite.base.imageData.MediumHandle

/**
 * An UndoContext is at its heart simple a Storage Structure for UndoActions, however their iterative behavior can
 * differ wildly depending on implementation
 * */
interface UndoContext<T> where T : UndoableAction {
    val medium: MediumHandle?

    fun addAction( action: T)
    fun undo()
    fun redo()

    val lastAction : T

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
