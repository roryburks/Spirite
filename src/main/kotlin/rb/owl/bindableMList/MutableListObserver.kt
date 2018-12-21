package rb.owl.bindableMList

import rb.owl.*

class ListPermuation(
    val startIndex: Int,
    val endIndex: Int,
    private val data: IntArray)
{
    operator fun get(index: Int) = when (index) {
        in startIndex..(endIndex - 1) -> data[index- startIndex]
        else -> index
    }
}

data class ListChange<T>(val index: Int, val new: T)

interface IListTriggers<T> {
    fun elementsAdded(index: Int, elements: Collection<T>)
    fun elementsRemoved(elements: Collection<T>)
    fun elementsChanged( changes: Set<ListChange<T>>)
    fun elementsPermuted( permutation: ListPermuation)
}


typealias IMutableListObserver<T> = IObserver<IListTriggers<T>>
typealias IMutableListObservable<T> = IObservable<IListTriggers<T>>

fun <T> IListTriggers<T>.observer() = MutableListObserver(this)

class MutableListObserver<T>(override val trigger: IListTriggers<T>) :
    IMutableListObserver<T>