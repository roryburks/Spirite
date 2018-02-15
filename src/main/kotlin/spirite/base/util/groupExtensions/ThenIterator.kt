package spirite.base.util.groupExtensions

private class ThenIterable<T>
    constructor(
            val first: Iterable<T>,
            val second: Iterable<T>
    ): Iterable<T> {
    override fun iterator(): Iterator<T> = ThenIterator()

    inner class ThenIterator: Iterator<T> {
        var iterator = first.iterator()
        var doneFirst = false

        override fun hasNext(): Boolean {
            return when {
                iterator.hasNext() -> true
                doneFirst -> false
                else -> {
                    iterator = second.iterator()
                    doneFirst = true
                    iterator.hasNext()
                }
            }
        }

        override fun next(): T {
            return when {
                iterator.hasNext() || doneFirst -> iterator.next()
                else -> {
                    iterator = second.iterator()
                    doneFirst = true
                    iterator.next()
                }
            }
        }
    }
}



fun <T> Iterable<T>.then( after: Iterable<T>) : Iterable<T> = ThenIterable(this, after)