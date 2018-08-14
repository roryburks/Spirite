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
private class PlusOneIterable<T>
constructor(
        val first: Iterable<T>,
        val then: T
): Iterable<T> {
    override fun iterator(): Iterator<T> = ThenIterator()

    inner class ThenIterator: Iterator<T> {
        var iterator = first.iterator()
        var doneThen = false

        override fun hasNext(): Boolean {
            return when {
                doneThen -> false
                else -> true
            }
        }

        override fun next(): T {
            return when {
                doneThen -> throw IndexOutOfBoundsException()
                iterator.hasNext() -> iterator.next()
                else -> {
                    doneThen = true
                    then
                }
            }
        }
    }
}



fun <T> Iterable<T>.then( after: Iterable<T>) : Iterable<T> = ThenIterable(this, after)
fun <T> Iterable<T>.then( t: T) : Iterable<T> = PlusOneIterable(this, t)