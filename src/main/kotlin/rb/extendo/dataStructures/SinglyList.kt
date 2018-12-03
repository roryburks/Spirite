package rb.extendo.dataStructures

class SinglyList<T>( val singly : T) : List<T> {
    override val size: Int get() = 1

    override fun contains(element: T) = (singly == element)
    override fun containsAll(elements: Collection<T>) = elements.all { it == singly }

    override fun get(index: Int) = when(index) {
        0 -> singly
        else -> throw IndexOutOfBoundsException("Singly List only has a Single Element")
    }

    override fun indexOf(element: T) = when(element) {
        singly -> 0
        else -> -1
    }

    override fun isEmpty() = false

    override fun iterator(): Iterator<T> = SinglyIterator()

    override fun lastIndexOf(element: T)= if( element == singly) 0 else -1

    override fun listIterator(): ListIterator<T> =SinglyListIterable()

    override fun listIterator(index: Int): ListIterator<T> = when {
        index == 1 -> SinglyListIterable()
        else -> throw RuntimeException("bad list bounds")
    }

    override fun subList(fromIndex: Int, toIndex: Int) = when {
        fromIndex == 0 && toIndex == 1 -> this
        else -> throw RuntimeException("bad list bounds")
    }

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

    inner class SinglyListIterable : ListIterator<T>{
        var done = false

        override fun hasNext() = !done
        override fun hasPrevious(): Boolean = done
        override fun next(): T = when {
            !done -> {
                done = true
                singly
            }
            else -> throw RuntimeException("No Next")
        }

        override fun nextIndex() = when {
            done -> 1
            else -> 0
        }

        override fun previous(): T  = when {
            done -> {
                done = true
                singly
            }
            else -> throw RuntimeException("No Next")
        }

        override fun previousIndex(): Int = when {
            done -> 0
            else -> -1
        }
    }
}
