package sjunit.testHelpers

import rb.owl.Observable
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.undo.UndoIndex
import spirite.base.imageData.undo.UndoableAction

/// Passtrough Undo Engine has no undo functionality, but allows commands that are told
/// to executre through to execute.
object PassthroughUndoEngine  : IUndoEngine {
    override fun reset() { }

    override var queuePosition: Int = 0
    override val dataUsed: Set<MediumHandle> = emptySet()
    override fun setSaveSpot() { }
    override val isAtSaveSpot: Boolean get() = true
    override val undoHistory: List<UndoIndex> = emptyList()

    override fun performAndStore(action: UndoableAction) {
        action.performAction()
        action.onAdd()
    }

    override fun doAsAggregateAction(description: String, stackable: Boolean, runner: () -> Unit) {
        runner()
    }

    override fun undo(): Boolean = true
    override fun redo(): Boolean = true
    override fun cleanup() { }
    override val undoHistoryObserver = Observable<(IUndoEngine.UndoHistoryChangeEvent) -> Any?>()
}
