package rbJvm.owl

import rb.IContract
import rb.extendo.dataStructures.SinglySequence
import rb.extendo.extensions.mapRemoveIfNull
import rb.owl.IObservable
import rb.owl.IObserver
import rb.owl.bindable.Bindable
import rb.owl.bindable.IBindObserver
import rb.owl.bindable.IBindable
import rb.owl.bindable.OnChangeEvent
import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

/** A WeakObserver is an Observer that does not have a strong reference to the trigger.  The idea being that the trigger
 * will have strong references to several potentially-large objects.  Someone else should keep a firm rederence to the
 * trigger.
 */
class WeakObserver<T>(trigger: T) : IObserver<T>
{
    val description = trigger.toString()
    private val weakTrigger = WeakReference(trigger)
    override val triggers : Sequence<T>? get() =   weakTrigger.get()?.run { SinglySequence(this) }
            ?: null.also{ println("$description fallen out of workspace.")}
}

/** Note: the contract strongly references the Trigger, so the t is preserved as long as the contract is. */
fun <T> IObservable<T>.addWeakObserver(t: T, trigger: Boolean = true) : IContract =
        WeakObserverContract(this.addObserver(WeakObserver(t)), t)

/** Note: the contract strongly references the Trigger, so the trigger is preserved as long as the contract is. */
fun <T> IBindable<T>.addWeakObserver(trigger: Boolean = true, t: (new: T, old: T)->Unit) : IContract =
        WeakObserverContract(this.addObserver(WeakObserver(t)),t)

private class WeakObserverContract<T>(private val bindContract: IContract, t: T) : IContract {
    var t: T? = t
    override fun void() {
        bindContract.void()
        t = null
    }
}

class WeakBindable<T>(default: T) : IObservable<OnChangeEvent<T>>
{
    val bind = Bindable(default)

    override fun addObserver(observer: IBindObserver<T>, trigger: Boolean): IContract = ObserverContract(observer)
    fun bindTo( root: Bindable<T>) : IContract = BindContract(root)

    private var externalTo: T = default
    private var internalTo: T = default

    private inner class BindContract( val externalBind: Bindable<T>) : IContract {
        init {bind.field = externalBind.field}
        val bindToWeakTrigger = externalBind.addWeakObserver { new, _ ->
            if( internalTo != new) {
                internalTo = new
                bind.field = new
            }
        }
        val weakToBindTrigger = bind.addWeakObserver { new, _ ->
            if( externalTo != new) {
                externalTo = new
                externalBind.field = new
            }
        }
        override fun void() {
            bindToWeakTrigger.void()
            weakToBindTrigger.void()
        }
    }
    
    private inner class ObserverContract(val observer: IBindObserver<T>) : IContract {
        init {observers.add(observer)}
        override fun void() {observers.remove(observer)}
    }

    private val binds = mutableListOf<BindContract>()
    //private val triggers get() = observers.mapRemoveIfNull { it.triggers }
    private val observers = mutableListOf<IBindObserver<T>>()

    operator fun getValue(thisRef: Any, prop: KProperty<*>): T = bind.field
    operator fun setValue(thisRef:Any, prop: KProperty<*>, value: T) {bind.field = value}
}

fun <T> Bindable<T>.bindWeaklyTo(root: Bindable<T>) : IContract
{
    this.field = root.field
    val weakBind = WeakBindable(root.field)
    return DoubleContract(
            WeakObserverContract(weakBind.bindTo(root),root),
            WeakObserverContract(weakBind.bindTo(this),this))
}

private class DoubleContract(contract1: IContract, contract2: IContract) : IContract
{
    var c1 : IContract? = contract1
    var c2: IContract? = contract2
    override fun void() {
        c1?.void()
        c2?.void()
        c1 = null
        c2 = null
    }
}