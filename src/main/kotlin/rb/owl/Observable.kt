package rb.owl

import rb.IContract
import rb.extendo.dataStructures.SinglySequence


interface IObserver<T> {
    val triggers: Sequence<T>?
}

class Observer<T>( val trigger: T) : IObserver<T>{
    override val triggers: Sequence<T>? get() = SinglySequence(trigger)
}
fun <T> T.observer() = Observer(this)

interface IObservable<T> {
    fun addObserver( observer: IObserver<T>, trigger: Boolean = true) : IContract
}

class Observable<T> : IObservable<T>
{
    override fun addObserver(observer: IObserver<T>, trigger: Boolean): IContract {
        return MetaContract(observer)
    }

    fun trigger(lambda : (T)->Unit) {
        observers.removeAll { it.observer.triggers ?.forEach(lambda) == null }
    }

    private val observers = mutableListOf<MetaContract>()

    private inner class MetaContract(val observer: IObserver<T>) : IContract {
        init {observers.add(this)}
        override fun void() {observers.remove(this)}
    }
}

fun <T> Observable<T>.addObserver(trigger: T) : IContract = this.addObserver(trigger.observer())

