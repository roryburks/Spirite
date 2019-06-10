package rb.owl

import rb.IContract


interface IObserver<T> {
    val trigger: T?
}

class Observer<T>(override val trigger: T) : IObserver<T>
fun <T> T.observer() = Observer(this)

interface IObservable<T> {
    fun addObserver( observer: IObserver<T>, trigger: Boolean = true) : IContract
}

class Observable<T> : IObservable<T>
{
    override fun addObserver(observer: IObserver<T>, trigger: Boolean): IContract = MetaContract(observer)

    fun trigger(lambda : (T)->Unit) {
        observers.forEach {it.observer.trigger?.apply(lambda)}
        observers.removeAll { it.observer.trigger == null }
    }

    private val observers = mutableListOf<MetaContract>()

    private inner class MetaContract(val observer: IObserver<T>) : IContract {
        init {observers.add(this)}
        override fun void() {observers.remove(this)}
    }
}