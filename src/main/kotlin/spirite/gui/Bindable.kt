package spirite.gui

import java.lang.ref.WeakReference
import kotlin.reflect.KProperty


/***
 * Bindable objects can be bound to other Bindables of the same type so that they share the same underlying scroll.
 * Bindables come with an optional onChange Lambda that will be invoked whenever any bound Bindable is changed (thus
 * the underlying scroll is changed).  It will also trigger when a Bindable is bound to another Bindable (so long as their
 * underlying is different)
 */
class Bindable<T>( defaultValue: T, var onChange: ((T)->Unit)? = null) {
    var field: T
        get() = underlying.field
        set(value) {underlying.field = value}

    private var underlying = BindableUnderlying(this,defaultValue)

    /** Note: Calling b1.bind( b2) will result in both having b2's current underlying scroll. */
    fun bind( root: Bindable<T>) {
        if( root.underlying != underlying) {
            root.underlying.swallow(underlying)
            underlying = root.underlying
        }
    }

    fun addListener( listener: (T)->Unit) {
        underlying.swallow( Bindable(field, listener).underlying)
        listener.invoke(field)
    }

    fun unbind() {
        val value = field
        underlying.detatch(this)
        underlying = BindableUnderlying(this,value)
    }

    private class BindableUnderlying<T>( bindable: Bindable<T>, defaultValue: T) {
        var field : T = defaultValue
            set(value) {
                if( value != field) {
                    field = value
                    bindings.forEach { it.onChange?.invoke(value) }
                }
            }
        private val bindings = mutableListOf<Bindable<T>>(bindable)

        fun swallow( other: BindableUnderlying<T>) {
            // Note: since the new underlying's field might be different to the old one, an onChange trigger might be needed
            if( other.field != field)
                other.bindings.forEach { it.onChange?.invoke(field) }
            bindings.addAll( other.bindings)
        }
        fun detatch( toRemove: Bindable<T>) {
            bindings.remove(toRemove)
        }
    }

    operator fun getValue(thisRef: Any, prop: KProperty<*>): T = field
    operator fun setValue(thisRef:Any, prop: KProperty<*>, value: T) {field = value}

    class Bound<T>(private val bindable: Bindable<T>) {
        operator fun getValue(thisRef: Any, prop: KProperty<*>): T = bindable.field
        operator fun setValue(thisRef:Any, prop: KProperty<*>, value: T) {bindable.field = value}
    }
}
