package rb.owl.bindableMList

import rb.global.IContract
import rb.extendo.extensions.mapRemoveIfNull


interface IBindableMList<T> {
    val list: List<T>
    fun addObserver( observer: IMutableListObserver<T>, trigger: Boolean = false) : IContract
}

class BindableMList<T>(col: Collection<T> = emptyList()) : IBindableMList<T>
{
    // region Public
    override val list: MutableList<T> get() = underlying.list

    override fun addObserver(observer: IMutableListObserver<T>, trigger: Boolean) : IContract = ObserverContract(observer)
        .also { if( trigger) observer.triggers?.forEach { it.elementsAdded(0, list.toList()) }}

    fun bindTo( root: BindableMList<T>) : IContract
    {
        val oldUnderlying = underlying
        val newUnderlying = root.underlying
        if( oldUnderlying != newUnderlying){
            oldUnderlying.triggers
                    .flatten()
                    .forEach {
                        it.elementsRemoved(oldUnderlying.list)
                        it.elementsAdded(0, newUnderlying.list)
                    }
        }
        bindList.add(root)

        oldUnderlying.bindings.remove(this)
        newUnderlying.bindings.add(this)

        val oldBinds = HashSet<BindableMList<T>>()
        fun travelOld(current: BindableMList<T>)
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

    private var underlying = Underlying(col, this)

    private val triggers get() = observers.mapRemoveIfNull{it.triggers}
    private val observers = mutableListOf<IMutableListObserver<T>>()
    private val bindList = mutableListOf<BindableMList<T>>()


    private class Underlying<T>(col: Collection<T>, root: BindableMList<T>) {
        val list = ObservableMList(col)

        init {
            list.addObserver(object : IListTriggers<T> {
                override fun elementsAdded(index: Int, elements: Collection<T>)
                {triggers.flatten().forEach { it.elementsAdded(index, elements) }}
                override fun elementsRemoved(elements: Collection<T>)
                {triggers.flatten().forEach { it.elementsRemoved(elements) }}
                override fun elementsChanged(changes: Set<ListChange<T>>)
                {triggers.flatten().forEach { it.elementsChanged(changes) }}
                override fun elementsPermuted(permutation: ListPermuation)
                {triggers.flatten().forEach { it.elementsPermuted(permutation) }}
            }.observer())
        }

        val triggers get() = bindings.asSequence().flatMap { it.triggers }
        val bindings = hashSetOf(root)
    }

    private inner class ObserverContract(val observer: IMutableListObserver<T>) :
        IContract {
        init {observers.add(observer) }
        override fun void() {observers.remove(observer)}
    }

    private inner class BindingContract(val root: BindableMList<T>) : IContract {
        override fun void() {
            val rootBinds = hashSetOf<BindableMList<T>>()
            val derivedBinds = hashSetOf<BindableMList<T>>()

            this@BindableMList.bindList.remove(root)

            fun travelRoot( current: BindableMList<T>)
            {
                if( rootBinds.contains(current)) return
                rootBinds.add(current)
                current.bindList.forEach { travelRoot(it) }
            }

            fun travelDerived_ReturnTrueIfConnected(current: BindableMList<T>) : Boolean
            {
                if( rootBinds.contains(current)) return true
                if( derivedBinds.contains(current)) return false
                derivedBinds.add(current)
                return current.bindList.any { travelDerived_ReturnTrueIfConnected(it) }
            }

            travelRoot( root)
            if( !travelDerived_ReturnTrueIfConnected(this@BindableMList)) {
                // Note: even though the subFun is short-circuiting on-true, on-false it will have completely filled derivedBinds
                val newUnderlying = Underlying(underlying.list, this@BindableMList)
                val oldUnderlying = underlying
                oldUnderlying.bindings.removeAll {derivedBinds.contains(it) }
                newUnderlying.bindings.addAll(derivedBinds)
            }
        }
    }
}