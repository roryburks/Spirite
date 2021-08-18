package spirite.base.imageData.undo

/**
 * IUndoEngineFeed is an interface for components which only uses IUndoEngine as a report destination for changes
 * happening to it.  And in particular, this report sources often view the
 */
interface IUndoEngineFeed {
    fun performAndStore( action: UndoableAction)
    fun doAsAggregateAction( description: String, stackable: Boolean = false, runner: () -> Unit)
}

fun IUndoEngineFeed?.performAndStore( action: UndoableAction) {
    if( this == null){
        action.onAdd()
        action.performAction()
    }
    else {
        this.performAndStore(action)
    }
}

fun IUndoEngineFeed?.doAsAggregateAction( description: String, stackable: Boolean = false, runner: () -> Unit)
{
    if( this == null){
        runner.invoke()
    }
    else {
        this.doAsAggregateAction(description, stackable, runner)
    }
}