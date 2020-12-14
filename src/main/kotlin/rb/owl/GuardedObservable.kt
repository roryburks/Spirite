package rb.owl

import rb.global.IContract

class GuardedObservable<T> : IObservable<T>
{
    private var _isRunning: Boolean = false
    private val _observers = mutableListOf<Contract>()

    override fun addObserver(observer: IObserver<T>, trigger: Boolean): IContract = Contract(observer)

    fun trigger(lambda : (T)->Unit) {
        if( !_isRunning) {
            _isRunning = true
            try {
                _observers.removeAll { it.observer.triggers?.forEach(lambda) == null }
            }
            finally {
                _isRunning = false
            }
        }
    }

    private inner class Contract(val observer: IObserver<T>) : IContract {
        init {_observers.add(this)}
        override fun void() {_observers.remove(this)}
    }

}