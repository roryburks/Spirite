package spirite.base.util.binding

import java.lang.ref.WeakReference
import kotlin.reflect.KProperty


typealias CruddyOldOnChangeEvent<T> = (new: T, old:T)->Unit

// ICruddyOldBindable<T> is essentially a read-only access of a CruddyBindable, but it can be wrapped for various purposes (such as
//  deciding which among many bindables the user needs at a given moment)
interface ICruddyOldBindable<T> {
    val field: T
    fun addListener( listener: CruddyOldOnChangeEvent<T>) : ICruddyBoundListener<T>
    fun addWeakListener( listener: CruddyOldOnChangeEvent<T>) : ICruddyBoundListener<T>
}

interface MCruddyOldBindable<T> : ICruddyOldBindable<T> {
    override var field: T
    fun bind( derived: CruddyBindable<T>)
    fun bindWeakly( derived: CruddyBindable<T>)
}

// When you create a Listener not bound to an existing CruddyBindable, sometimes you'll want to remove that listener manually.
//  That's what this interface is for.
interface ICruddyBoundListener<T> {
    fun unbind()
}

/***
 * CruddyBindable objects can be bound to other Bindables of the same type so that they share the same underlying scroll.
 * Bindables come with an optional onChange Lambda that will be invoked whenever any bound CruddyBindable is changed (thus
 * the underlying scroll is changed).  It will also trigger when a CruddyBindable is bound to another CruddyBindable (so long as their
 * underlying is different)
 */
class CruddyBindable<T>(defaultValue: T, var onChange: CruddyOldOnChangeEvent<T>? = null) : MCruddyOldBindable<T>, ICruddyBoundListener<T> {
    override var field: T
        get() = underlying.field
        set(value) {underlying.field = value}

    private var underlying = BindableUnderlying(this, defaultValue)

    /** Root listeners do not disappear when you call this.unbind()*/
    fun addRootListener(listener: CruddyOldOnChangeEvent<T>) {
        val oldOnChange = onChange
        when( oldOnChange) {
            null -> onChange = listener
            else -> onChange = {new,old-> oldOnChange.invoke(new, old) ; listener.invoke(new,old)}
        }
        listener.invoke(field, field)
    }

    /** Note: Calling b1.bind( b2) will result in both having b2's current underlying scroll. */
    override fun bind( derived: CruddyBindable<T>) {
        if( derived.underlying != underlying) {
            underlying.swallow(derived.underlying)
            derived.underlying = underlying
        }
    }

    override fun bindWeakly( derived: CruddyBindable<T>) {
        if( derived.underlying != underlying) {
            underlying.swallowWeakly(derived.underlying)
            derived.underlying = underlying

        }
    }

    override fun addListener(listener: CruddyOldOnChangeEvent<T>) : ICruddyBoundListener<T> {
        val bind = CruddyBindable(field, listener)
        underlying.swallow( bind.underlying)
        listener.invoke(field, field)
        return bind
    }

    override fun addWeakListener(listener: CruddyOldOnChangeEvent<T>) : ICruddyBoundListener<T> {
        val bind = CruddyBindable(field, listener)
        underlying.swallowWeakly( bind.underlying)
        listener.invoke(field, field)
        return bind
    }

    override fun unbind() {
        val value = field
        underlying.detatch(this)
        underlying = BindableUnderlying(this, value)
    }

    private class BindableUnderlying<T>(bindable: CruddyBindable<T>, defaultValue: T) {
        var field : T = defaultValue
            set(value) {
                val prev = field
                if( value != field) {
                    field = value
                    bindings.forEach { it.onChange?.invoke(value, prev) }
                    weakBindings.removeIf { it.get() == null }
                    weakBindings.forEach { it.get()?.onChange?.invoke(value, prev) }
                }
            }
        private val bindings = mutableListOf<CruddyBindable<T>>(bindable)
        val weakBindings = mutableListOf<WeakReference<CruddyBindable<T>>>()

        fun swallow( other: BindableUnderlying<T>) {
            // Note: since the new underlying's field might be different to the old one, an onChange trigger might be needed
            if( other.field != field) {
                other.bindings.forEach { it.onChange?.invoke(field, other.field) }
                other.weakBindings.removeIf { it.get() == null }
                other.weakBindings.forEach { it.get()?.onChange?.invoke(field, other.field) }
            }
            bindings.addAll( other.bindings)
            weakBindings.addAll(other.weakBindings)
        }
        fun swallowWeakly( other: BindableUnderlying<T>) {
            // Note: since the new underlying's field might be different to the old one, an onChange trigger might be needed
            if( other.field != field) {
                other.bindings.forEach { it.onChange?.invoke(field, other.field) }
                other.weakBindings.removeIf { it.get() == null }
                other.weakBindings.forEach { it.get()?.onChange?.invoke(field, other.field) }
            }
            weakBindings.addAll(other.weakBindings)
            weakBindings.addAll( other.bindings.map { WeakReference(it) })
        }
        fun detatch( toRemove: CruddyBindable<T>) {
            bindings.remove(toRemove)
            weakBindings.removeIf { it.get() == null || it.get() == toRemove }
        }
    }

    operator fun getValue(thisRef: Any, prop: KProperty<*>): T = field
    operator fun setValue(thisRef:Any, prop: KProperty<*>, value: T) {field = value}
}