package rb.extendo.dataStructures

class SinglySequence<T>(private val single: T) : Sequence<T>
{
    override fun iterator(): Iterator<T> = SinglyIterator(single)
}

class SinglyCollection<T>(private val single: T) : Collection<T>
{
    override val size: Int get() = 1
    override fun contains(element: T) = single == element
    override fun containsAll(elements: Collection<T>) = elements.all { it == single }
    override fun isEmpty() = false
    override fun iterator() = SinglyIterator(single)
}

class SinglyIterator<T>(private val single: T) : Iterator<T>
{
    var done = false
    override fun hasNext() = !done
    override fun next(): T {
        if( done) throw IndexOutOfBoundsException()
        done = true
        return single
    }
}