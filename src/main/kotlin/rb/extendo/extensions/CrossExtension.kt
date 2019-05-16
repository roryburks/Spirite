package rb.extendo.extensions

fun <A,B> List<A>.cross( bs: List<B>) : List<Pair<A,B>> = CrossList(this, bs)
fun <A,B> Iterable<A>.cross( bs: Iterable<B>) : Iterable<Pair<A,B>> = CrossIterable(this, bs) { a, b-> Pair(a,b) }

fun <A,B,R> Iterable<A>.cross( bs: Iterable<B>, transform: (A, B) -> R) : Iterable<R> = CrossIterable(this, bs, transform)

class CrossIterable<A,B,R>(
    private val ait: Iterable<A>,
    private val bit: Iterable<B>,
    private val transform: (A, B) -> R)
    :Iterable<R>
{
    override fun iterator(): Iterator<R>  = IteratorImp()

    private inner class IteratorImp : Iterator<R> {
        private var a_ = ait.iterator()
        private val b_ = bit.iterator()
        private var b : B? = null


        override fun hasNext() = when {
            a_.hasNext() -> true
            b_.hasNext() -> {
                b = b_.next()
                a_ = ait.iterator()
                a_.hasNext()
            }
            else -> false
        }

        override fun next(): R {
            if( a_.hasNext()) {
                val _b = b ?: b_.next().also { b = it }
                return transform(a_.next(), _b)
            }
            else {
                val _b = b_.next().also { b = it }
                a_ = ait.iterator()
                return transform(a_.next(), _b)
            }
        }
    }
}


class CrossList<A,B>(
    private val aList: List<A>,
    private val bList: List<B>)
    : List<Pair<A,B>>
{
    override val size: Int get() = aSize * bSize
    private val aSize get() = aList.size
    private val bSize get() = bList.size

    override fun contains(element: Pair<A, B>) =
        aList.contains(element.first) && bList.contains(element.second)

    override fun containsAll(elements: Collection<Pair<A, B>>): Boolean {
        return elements.all { aList.contains(it.first) } && elements.all { bList.contains(it.second) }
    }

    override fun get(index: Int): Pair<A, B> {
        if( index < 0 || index >= size) throw IndexOutOfBoundsException("index")

        val a = index / bSize
        val b = index % bSize
        return Pair(aList[a], bList[b])
    }

    override fun indexOf(element: Pair<A, B>): Int {
        val a = aList.indexOf(element.first)
        if( a == -1) return -1
        val b = bList.indexOf(element.second)
        if( b == -1) return -1
        return b + a*bList.size
    }

    override fun isEmpty() = aList.isEmpty() && bList.isEmpty()

    override fun lastIndexOf(element: Pair<A, B>): Int {
        val a = aList.lastIndexOf(element.first)
        if( a == -1) return -1
        val b = bList.lastIndexOf(element.second)
        if( b == -1) return -1
        return b + a*bList.size
    }

    override fun subList(fromIndex: Int, toIndex: Int) =
        CrossIterator(fromIndex).run { List(toIndex - fromIndex + 1){next()} }

    override fun iterator(): Iterator<Pair<A, B>> = CrossIterator()
    override fun listIterator(): ListIterator<Pair<A, B>> = CrossIterator()
    override fun listIterator(index: Int): ListIterator<Pair<A, B>> = CrossIterator(index)


    private inner class CrossIterator(startIndex : Int = 0) : ListIterator<Pair<A,B>>
    {
        private var a = startIndex / bSize
        private var b = startIndex % bSize

        override fun hasNext() = size > a*bSize + b-1
        override fun hasPrevious() = a*bSize + b > 0

        override fun nextIndex() = bSize*a + b + 1
        override fun previousIndex(): Int = bSize*a + b - 1

        override fun next(): Pair<A, B> {
            val res = Pair(aList[a], bList[b++])
            if( b == bList.size) {
                b = 0
                ++a
            }
            return res
        }

        override fun previous(): Pair<A, B> {
            val res = Pair(aList[a], bList[b--])
            if( b == -1) {
                b = 0
                --a
            }
            return res
        }
    }
}