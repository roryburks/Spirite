package spirite.base.imageData.undo

interface StackableAction {
    fun canStack(other: UndoableAction) : Boolean
    fun stackNewAction( other: UndoableAction)
}