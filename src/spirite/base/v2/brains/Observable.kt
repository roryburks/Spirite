package spirite.base.v2.brains

import java.lang.ref.WeakReference

/**
 * Note: the IObservable interface shields users of the Add and Remove features from being able to trigger observed events
 * and other internal functionality.
 */
interface IObservable<T> {
    fun addObserver( toAdd: T)
    fun removeObserver( toRemove: T)
}

class Observable<T> : IObservable<T>
{
    private val observers = mutableListOf<WeakReference<T>>()

    override fun addObserver(toAdd: T) {
        observers.add( WeakReference(toAdd))
    }

    override fun removeObserver(toRemove: T) {
        observers.removeIf {
            val obs = it.get()
            obs == null || obs == toRemove
        }
    }

    fun trigger( trigger: (T) -> Unit ) {
        // This is probably bad style, but avoids double-iterating
        observers.removeIf {
            val obs = it.get()
            if( obs != null)
                trigger(obs)
            obs == null
        }
    }
}