package spirite.base.util.delegates

import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.undo.NullAction
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
        else {
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