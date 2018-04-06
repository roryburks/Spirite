package spirite.base.brains

import java.lang.ref.WeakReference
import kotlin.reflect.KProperty


typealias OnChangeEvent<T> = (new: T, old:T)->Unit

// IBindable<T> is essentially a read-only access of a Bindable, but it can be wrapped for various purposes (such as
//  deciding which among many bindables the user needs at a given moment)
interface IBindable<T> {
    val field: T
    fun addListener( listener: OnChangeEvent<T>) : IBoundListener<T>
    fun addWeakListener( listener: OnChangeEvent<T>) : IBoundListener<T>
}

interface MBindable<T> : IBindable<T>{
    override var field: T
    fun bind( root: Bindable<T>)
    fun bindWeakly( root: Bindable<T>)
}

// When you create a Listener not bound to an existing Bindable, sometimes you'll want to remove that listener manually.
//  That's what this interface is for.
interface IBoundListener<T> {
    fun unbind()
}

/***
 * Bindable objects can be bound to other Bindables of the same type so that they share the same underlying scroll.
 * Bindables come with an optional onChange Lambda that will be invoked whenever any bound Bindable is changed (thus
 * the underlying scroll is changed).  It will also trigger when a Bindable is bound to another Bindable (so long as their
 * underlying is different)
 */
class Bindable<T>( defaultValue: T, var onChange: OnChangeEvent<T>? = null) : MBindable<T>, IBoundListener<T>{
    override var field: T
        get() = underlying.field
        set(value) {underlying.field = value}

    private var underlying = BindableUnderlying(this, defaultValue)

    /** Note: Calling b1.bind( b2) will result in both having b2's current underlying scroll. */
    override fun bind( root: Bindable<T>) {
        if( root.underlying != underlying) {
            root.underlying.swallow(underlying)
            underlying = root.underlying
        }
    }

    override fun bindWeakly( root: Bindable<T>) {
        if( root.underlying != underlying) {
            root.underlying.swallowWeakly(underlying)
            underlying = root.underlying
        }
    }

    override fun addListener(listener: OnChangeEvent<T>) : IBoundListener<T> {
        val bind = Bindable(field, listener)
        underlying.swallow( bind.underlying)
        listener.invoke(field, field)
        return bind
    }

    override fun addWeakListener(listener: OnChangeEvent<T>) : IBoundListener<T> {
        val bind = Bindable(field, listener)
        underlying.swallowWeakly( bind.underlying)
        listener.invoke(field, field)
        return bind
    }

    override fun unbind() {
        val value = field
        underlying.detatch(this)
        underlying = BindableUnderlying(this, value)
    }

    private class BindableUnderlying<T>(bindable: Bindable<T>, defaultValue: T) {
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
        private val bindings = mutableListOf<Bindable<T>>(bindable)
        val weakBindings = mutableListOf<WeakReference<Bindable<T>>>()

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
        fun detatch( toRemove: Bindable<T>) {
            bindings.remove(toRemove)
            weakBindings.removeIf { it.get() == null || it.get() == toRemove }
        }
    }

    operator fun getValue(thisRef: Any, prop: KProperty<*>): T = field
    operator fun setValue(thisRef:Any, prop: KProperty<*>, value: T) {field = value}

    class Bound<T>(private val bindable: Bindable<T>) {
        operator fun getValue(thisRef: Any, prop: KProperty<*>): T = bindable.field
        operator fun setValue(thisRef:Any, prop: KProperty<*>, value: T) {bindable.field = value}
    }
}
