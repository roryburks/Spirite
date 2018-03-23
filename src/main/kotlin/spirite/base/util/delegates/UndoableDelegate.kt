package spirite.base.util.delegates

import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.undo.StackableAction
import spirite.base.imageData.undo.UndoableAction
import kotlin.reflect.KProperty


class UndoableDelegate<T>(
        defaultValue : T,
        val undoEngine: IUndoEngine?,
        val changeDescription: String)
{
    var field = defaultValue

    operator fun getValue(thisRef: Any, prop: KProperty<*>): T = field

    operator fun setValue(thisRef:Any, prop: KProperty<*>, value: T) {
        if( undoEngine == null) field = value
        else if( field != value) {
            val oldValue = field
            val newValue = value
            undoEngine.performAndStore( object : NullAction() {
                override val description: String get() = changeDescription
                override fun performAction() {field = newValue}
                override fun undoAction() {field = oldValue}
            })
        }
    }
}

class StackableUndoableDelegate<T>(
        defaultValue : T,
        val undoEngine: IUndoEngine?,
        val changeDescription: String)
{
    var field = defaultValue

    operator fun getValue(thisRef: Any, prop: KProperty<*>): T = field

    operator fun setValue(thisRef:Any, prop: KProperty<*>, value: T) {
        when {
            undoEngine == null -> field = value
            field != value -> undoEngine.performAndStore(SUDAction(value,field))
        }
    }

    private inner class SUDAction(
            var newValue: T,
            val oldValue: T)
        : NullAction(), StackableAction
    {
        val context get() = this@StackableUndoableDelegate
        override val description: String get() = changeDescription
        override fun performAction() {field = newValue}
        override fun undoAction() {field=  oldValue}

        override fun canStack(other: UndoableAction): Boolean {
            return (other as? StackableUndoableDelegate<*>.SUDAction)?.context == context
        }

        override fun stackNewAction(other: UndoableAction) {
            ((other as? StackableUndoableDelegate<T>.SUDAction)?.newValue as? T)?.also { newValue = it }
        }

    }

}