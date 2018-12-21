package spirite.base.brains

import java.lang.ref.WeakReference

/**
 * Note: the ICruddyOldObservable interface shields users of the Add and Remove features from being able to trigger observed events
 * and other internal functionality.
 */
interface ICruddyOldObservable<T> {
    fun addObserver( toAdd: T) : T
    fun removeObserver( toRemove: T)
}

class CruddyOldObservable<T> : ICruddyOldObservable<T>
{
    private val observers = mutableListOf<WeakReference<T>>()

    override fun addObserver(toAdd: T) : T{
        observers.add( WeakReference(toAdd))
        return toAdd
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