package rbJvm.owl

import rb.IContract
import rb.owl.IObservable
import rb.owl.bindable.Bindable
import rb.owl.bindable.IBindObserver
import rb.owl.bindable.OnChangeEvent
import kotlin.reflect.KProperty

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
    return DoubleWeakContract(
            weakBind.bindTo(root),
            weakBind.bindTo(this),
            root, this)
}

private class DoubleWeakContract<T>(
        contract1: IContract,
        contract2: IContract,
        trigger1: T,
        trigger2: T) : IContract
{
    var c1 : IContract? = contract1
    var c2: IContract? = contract2
    var t1: T? = trigger1
    var t2: T? = trigger2
    override fun void() {
        c1?.void()
        c2?.void()
        c1 = null
        c2 = null
        t1 = null
        t2 = null
    }
}