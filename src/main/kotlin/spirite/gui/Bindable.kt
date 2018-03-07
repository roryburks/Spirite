package spirite.gui

import java.lang.ref.WeakReference
import kotlin.reflect.KProperty


class Bindable<T>( defaultValue: T, val onChange: ((T)->Unit)? = null) {
    var field: T
        get() = underlying.field
        set(value) {underlying.field = value}

    private var underlying = BindableUnderlying(this,defaultValue)

    fun bind( root: Bindable<T>) {
        root.underlying.swallow(underlying)
        underlying = root.underlying
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
                    bindings.removeIf { when( it.get()) {
                        null -> true
                        else -> {
                            it.get()?.onChange?.invoke(value)
                            false
                        }
                    } }
                }
            }
        private val bindings = mutableListOf<WeakReference<Bindable<T>>>(WeakReference(bindable))

        fun swallow( other: BindableUnderlying<T>) {
            bindings.addAll( other.bindings.filter { it.get() != null })
        }
        fun detatch( toRemove: Bindable<T>) {
            bindings.removeIf { it.get() ?: toRemove == toRemove }
        }
    }

    class Bound<T>( val bindable: Bindable<T>) {
        operator fun getValue(thisRef: Any, prop: KProperty<*>): T = bindable.field
        operator fun setValue(thisRef:Any, prop: KProperty<*>, value: T) {bindable.field = value}
    }
}

