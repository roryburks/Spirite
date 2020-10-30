package rb.owl.bindable

import rb.global.IContract
import rb.extendo.extensions.mapRemoveIfNull
import kotlin.reflect.KProperty

class Bindable<T>(default: T, private val mutator:  ((T)->T)? = null) : IBindable<T>
{
    // region Public
    override var field: T
        get() = underlying.value
        set(value) {underlying.value = mutator?.invoke(value) ?: value}

    override fun addObserver(observer: IBindObserver<T>, trigger: Boolean): IContract = ObserverContract(observer)
        .also { if( trigger) observer.triggers?.forEach { it(field,field) }}
    fun bindTo( root: Bindable<T>): IContract
    {
        val oldUnderlying = underlying
        val newUnderlying = root.underlying
        oldUnderlying.value = newUnderlying.value // Even though oldUnderlying will be discarded, this makes sure on-changes trigger
        bindList.add(root)

        oldUnderlying.bindings.remove(this)
        newUnderlying.bindings.add(this)

        val oldBinds = HashSet<Bindable<T>>()
        fun travelOld(current: Bindable<T>)
        {
            if( oldBinds.contains(current)) return
            oldBinds.add(current)
            current.bindList.forEach { travelOld(it) }
        }
        travelOld(this)
        oldBinds.forEach { it.underlying = newUnderlying }
        return BindingContract(root)
    }
    // endregion

    private var underlying = Underlying(default, this)

    private val triggers get() = observers
            .mapRemoveIfNull { it.triggers }
            .flatten()
    private val observers = mutableListOf<IBindObserver<T>>()
    private val bindList = mutableListOf<Bindable<T>>()

    private class Underlying<T>( default: T, root: Bindable<T>)  {
        var value: T = default
            set(value) {
                val prev = field
                if( value != field) {
                    field = value
                    val t = triggers.toList()
                    t.forEach { it.invoke(value,prev)}
                }
            }

        val triggers : Sequence<OnChangeEvent<T>>
            get() = bindings.asSequence().flatMap { it.triggers }
        val bindings = hashSetOf(root)   // Set avoids double-binding
    }

    private inner class ObserverContract( val observer: IBindObserver<T>) : IContract
    {
        init { observers.add(observer)}
        override fun void() {observers.remove(observer)}
    }

    private inner class BindingContract(val root: Bindable<T>) : IContract
    {
        override fun void() {
            val rootBinds = hashSetOf<Bindable<T>>()
            val derivedBinds = hashSetOf<Bindable<T>>()

            this@Bindable.bindList.remove(root)

            fun travelRoot(current: Bindable<T>)
            {
                if( rootBinds.contains(current)) return
                rootBinds.add(current)
                current.bindList.forEach { travelRoot(it) }
            }

            fun travelDerived_ReturnTrueIfConnected(current: Bindable<T>) : Boolean
            {
                if( rootBinds.contains(current)) return true
                if( derivedBinds.contains(current)) return false
                derivedBinds.add(current)
                return current.bindList.any { travelDerived_ReturnTrueIfConnected(it) }
            }

            travelRoot( root)
            if( !travelDerived_ReturnTrueIfConnected(this@Bindable)) {
                // Note: even though the subFun is short-circuiting on-true, on-false it will have completely filled derivedBinds
                val newUnderlying = Underlying(underlying.value, this@Bindable)
                val oldUnderlying = underlying
                oldUnderlying.bindings.removeAll {derivedBinds.contains(it) }
                newUnderlying.bindings.addAll(derivedBinds)
                underlying = newUnderlying
            }
        }
    }

    operator fun getValue(thisRef: Any, prop: KProperty<*>): T = field
    operator fun setValue(thisRef:Any, prop: KProperty<*>, value: T) {field = value}
}
