package rb.extendo.extensions


infix fun <T> Sequence<T>.then(other: Sequence<T>) = ThenSequence(this,other)
fun <T> Iterable<T>.then(other: Iterable<T>) : Iterable<T> = ThenIterable(this,other)

fun <T> Iterable<T>.then( t: T) : Iterable<T> = PlusOneIterable(this, t)

class ThenSequence<T>(
    private val first: Sequence<T>,
    private  val second: Sequence<T>)
    : Sequence<T>
{
    override fun iterator() = IteratorImp(first,second)

    class IteratorImp<T>(
        private val first: Sequence<T>,
        private val second: Sequence<T>)
        :Iterator<T>
    {
        private var onFirst = true
        private var iterator: Iterator<T>? = null

        override fun hasNext() : Boolean {
            val iterator = iterator
            return when {
                iterator == null -> {this.iterator = first.iterator(); hasNext()}
                onFirst && !iterator.hasNext() -> {
                    onFirst = false
                    this.iterator = second.iterator()
                    hasNext()
                }
                else -> iterator.hasNext()
            }
        }

        override fun next(): T  {
            val iterator = iterator
            return when {
                iterator == null -> {this.iterator = first.iterator() ; next()}
                onFirst && !iterator.hasNext() -> {
                    onFirst = false
                    this.iterator = second.iterator()
                    next()
                }
                else -> iterator.next()
            }
        }
    }
}

private class ThenIterable<T>(
    private val first: Iterable<T>,
    private val second: Iterable<T>)
    :Iterable<T>
{
    override fun iterator() = Imp()

    private inner class Imp : Iterator<T> {
        private var onFirst = true
        private var iterator: Iterator<T>? = null

        override fun hasNext(): Boolean {
            val iterator = iterator
            return when {
                iterator == null -> {
                    this.iterator = first.iterator(); hasNext()
                }
                onFirst -> {
                    onFirst = false
                    this.iterator = second.iterator()
                    hasNext()
                }
                else -> iterator.hasNext()
            }
        }

        override fun next(): T {
            val iterator = iterator
            return when {
                iterator == null -> {
                    this.iterator = first.iterator(); next()
                }
                onFirst && !iterator.hasNext() -> {
                    onFirst = false
                    this.iterator = second.iterator()
                    next()
                }
                else -> iterator.next()
            }
        }
    }
}


private class PlusOneIterable<T>
constructor(
        val first: Iterable<T>,
        val then: T)
    : Iterable<T>
{
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