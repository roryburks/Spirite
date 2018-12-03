package rb.extendo.dataStructures

class SinglySet<T>(val singly : T) : Set<T> {
    override val size: Int get() = 1

    override fun contains(element: T) = (element == singly)
    override fun containsAll(elements: Collection<T>) = elements.all { it == singly }

    override fun isEmpty() = false
    override fun iterator(): Iterator<T> = SinglyIterator()

    inner class SinglyIterator : Iterator<T> {
        var done = false
        override fun hasNext() = !done

        override fun next() = when {
            !done -> {
                done = true
                singly
            }
            else -> throw RuntimeException("No Next")
        }
    }
}