package spirite.base.util.binding

import java.lang.ref.WeakReference

abstract class BindableStub<T> : IBindable<T> {
    private val listeners = mutableListOf<OnChangeEvent<T>>()
    private val weakListeners = mutableListOf<WeakReference<OnChangeEvent<T>>>()

    protected fun trigger(new: T, old: T) {
        listeners.forEach { it.invoke(new, old) }
        weakListeners.removeIf { it.get()?.invoke(new,old) == null }
    }

    override fun addListener(listener: OnChangeEvent<T>) : IBoundListener<T> {
        return BoundStub(listener.apply { listeners.add( this) })
    }

    override fun addWeakListener(listener: OnChangeEvent<T>) : IBoundListener<T> {
        return BoundStub(listener.apply { weakListeners.add(WeakReference(this)) })
    }

    // Todo: this could potentially do weird things if someone binds the same oce multiple times
    inner class BoundStub<T>( private val oce: OnChangeEvent<T>) : IBoundListener<T> {
        override fun unbind() {
            listeners.removeIf { it == oce }
            weakListeners.removeIf { (it.get()?: oce) == oce }
        }
    }
}

abstract class MutableBindableStub<T> : BindableStub<T>(), MBindable<T> {
    private val listener = { new : T, old: T -> field = new}

    override fun bind(derived: Bindable<T>) {
        derived.field = field
        derived.addListener(listener)
    }

    override fun bindWeakly(derived: Bindable<T>) {
        derived.field = field
        derived.addWeakListener(listener)
    }
}