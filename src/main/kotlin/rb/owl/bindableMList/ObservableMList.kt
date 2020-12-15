package rb.owl.bindableMList

import rb.global.IContract
import rb.extendo.dataStructures.SinglyCollection
import rb.extendo.dataStructures.SinglySet
import rb.extendo.extensions.asHashSet
import rb.owl.IObserver


class ObservableMList<T>
    (list: Collection<T> = emptyList())
    : IMutableListObservable<T>, MutableList<T>
{
    private inner class ObserverContract(private val observer: IMutableListObserver<T>) :
        IContract {
        override fun void() { observers.remove(observer)}
    }

    override fun addObserver(observer: IObserver<IListTriggers<T>>, trigger: Boolean): IContract {
        observers.add(observer)
        if( trigger && list.any())
            observer.triggers?.forEach { it.elementsAdded(0, list)}
        return ObserverContract(observer)
    }

    private val list = list.toMutableList()
    private val observers = mutableListOf<IMutableListObserver<T>>()

    // Delegation
    override val size: Int get() = list.size
    override fun contains(element: T) = list.contains(element)
    override fun containsAll(elements: Collection<T>) = list.containsAll(elements)
    override fun get(index: Int) = list[index]
    override fun indexOf(element: T) = list.indexOf(element)
    override fun isEmpty() = list.isEmpty()
    override fun iterator() =list.iterator()
    override fun lastIndexOf(element: T) = list.lastIndexOf(element)
    override fun listIterator() = list.listIterator()
    override fun listIterator(index: Int) = list.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int) = list.subList(fromIndex, toIndex)

    // region add
    override fun add(element: T): Boolean {
        if( !list.add(element))
            return false
        val removed= SinglyCollection(element)
        observers.removeAll { it.triggers?.forEach { it.elementsAdded(list.lastIndex, removed)} == null }
        return true
    }
    override fun add(index: Int, element: T) {
        list.add(index, element)
        observers.removeAll { it.triggers?.forEach { it.elementsAdded(index, SinglyCollection(element))} == null }
    }
    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        if(!list.addAll(index, elements))
            return false
        observers.removeAll { it.triggers?.forEach { it.elementsAdded(index, elements)} == null }
        return true
    }
    override fun addAll(elements: Collection<T>): Boolean {
        val index = list.size
        if(!list.addAll(elements))
            return false
        observers.removeAll { it.triggers?.forEach { it.elementsAdded(index, elements) }== null }
        return true
    }
    // endregion

    // region Remove
    override fun clear() {
        val elements = list.toSet()
        list.clear()
        observers.removeAll { it.triggers?.forEach { it.elementsRemoved(elements)} == null }
    }
    override fun remove(element: T): Boolean {
        if( !list.remove(element))
            return false
        val removed = SinglyCollection(element)
        observers.removeAll { it.triggers?.forEach { it.elementsRemoved(removed) }== null }
        return true
    }
    override fun removeAll(elements: Collection<T>): Boolean {
        val hashed = elements.asHashSet()
        val removed = mutableSetOf<T>()
        list.removeAll {
            when(hashed.contains(it)) {
                true -> {
                    removed.add(it)
                    true
                }
                false -> false
            }}
        if( !removed.any()) return false
        observers.removeAll { it.triggers?.forEach { it.elementsRemoved(removed) } == null }
        return true
    }
    override fun removeAt(index: Int): T {
        return list.removeAt(index)
            .also { t ->
                val removed = SinglyCollection(t)
                observers.removeAll { it.triggers?.forEach { it.elementsRemoved(removed) }== null } }
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        val hashed = elements.asHashSet()
        val removed = mutableListOf<T>()
        list.removeAll {
            when(!hashed.contains(it)) {
                true -> {
                    removed.add(it)
                    true
                }
                false -> false
            }}
        if(!removed.any()) return false
        observers.removeAll { it.triggers?.forEach { it.elementsRemoved(removed) }== null }
        return true
    }
    // endregion

    override fun set(index: Int, element: T): T {
        val old = list.set(index, element)
        val changed = SinglySet(ListChange(index, element))
        observers.removeAll { it.triggers?.forEach { it.elementsChanged(changed) }== null }
        return old
    }

    fun setMany(change: Iterable<ListChange<T>>) : Set<ListChange<T>>{
        val oldSet =  change
            .map { (index, new) -> ListChange(index, list.set(index, new)) }
            .toSet()
        val asSet = change.toSet()
        observers.removeAll { it.triggers?.forEach { it.elementsChanged(asSet) }== null }
        return oldSet
    }

    fun permute(permuation: ListPermuation) : Set<ListChange<T>> {
        return setMany( (permuation.startIndex until permuation.endIndex).asIterable()
            .map { ListChange(permuation[it], list[it]) })
    }
 }