package spirite.base.util.binding

import java.lang.ref.WeakReference

abstract class BindableStub<T> : ICruddyOldBindable<T> {
    private val listeners = mutableListOf<CruddyOldOnChangeEvent<T>>()
    private val weakListeners = mutableListOf<WeakReference<CruddyOldOnChangeEvent<T>>>()

    protected fun trigger(new: T, old: T) {
        listeners.forEach { it.invoke(new, old) }
        weakListeners.removeIf { it.get()?.invoke(new,old) == null }
    }

    override fun addListener(listener: CruddyOldOnChangeEvent<T>) : ICruddyBoundListener<T> {
        return BoundStub(listener.apply { listeners.add( this) })
    }

    override fun addWeakListener(listener: CruddyOldOnChangeEvent<T>) : ICruddyBoundListener<T> {
        return BoundStub(listener.apply { weakListeners.add(WeakReference(this)) })
    }

    // Todo: this could potentially do weird things if someone binds the same oce multiple times
    inner class BoundStub<T>( private val oce: CruddyOldOnChangeEvent<T>) : ICruddyBoundListener<T> {
        override fun unbind() {
            listeners.removeIf { it == oce }
            weakListeners.removeIf { (it.get()?: oce) == oce }
        }
    }
}

abstract class MutableBindableStub<T> : BindableStub<T>(), MCruddyOldBindable<T> {
    private val listener = { new : T, old: T -> field = new}

    override fun bind(derived: CruddyBindable<T>) {
        derived.field = field
        derived.addListener(listener)
    }

    override fun bindWeakly(derived: CruddyBindable<T>) {
        derived.field = field
        derived.addWeakListener(listener)
    }
}