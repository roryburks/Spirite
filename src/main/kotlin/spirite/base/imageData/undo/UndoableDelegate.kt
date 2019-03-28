package spirite.base.imageData.undo

import kotlin.reflect.KProperty


class UndoableDelegate<T>(
        defaultValue : T,
        val undoEngine: IUndoEngine?,
        val changeDescription: String)
{
    var field = defaultValue

    operator fun getValue(thisRef: Any, prop: KProperty<*>): T = field

    operator fun setValue(thisRef:Any, prop: KProperty<*>, newValue: T) {
        if( undoEngine == null) field = newValue
        else if( field != newValue) {
            val oldValue = field
            undoEngine.performAndStore( object : NullAction() {
                override val description: String get() = changeDescription
                override fun performAction() {field = newValue}
                override fun undoAction() {field = oldValue}
            })
        }
    }
}

class UndoableChangeDelegate<T>(
        defaultValue : T,
        val undoEngine: IUndoEngine?,
        val changeDescription: String,
        val onChange: (T)->Any?)
{
    var field = defaultValue
        set(value) {
            if( value != field) {
                field = value
                onChange(value)
            }
        }

    operator fun getValue(thisRef: Any, prop: KProperty<*>): T = field

    operator fun setValue(thisRef:Any, prop: KProperty<*>, newValue: T) {
        if( undoEngine == null) field = newValue
        else if( field != newValue) {
            val oldValue = field
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
            return (other as? StackableUndoableDelegate<T>.SUDAction)?.context == context
        }

        override fun stackNewAction(other: UndoableAction) {
            ((other as? StackableUndoableDelegate<T>.SUDAction)?.newValue)?.also { newValue = it }
        }

    }

}