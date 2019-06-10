package rb.extendo.extensions

fun <From,To> MutableIterable<From>.mapRemoveIfNull(mapping: (From) -> To?) : Sequence<To>
        = MapRemoveIfNullSequence(this, mapping)

private class MapRemoveIfNullSequence<From,To>(
    val mutableIterable: MutableIterable<From>,
    val mapping: (From)->To?)
    : Sequence<To>
{
    override fun iterator(): Iterator<To> = IteratorImp(mutableIterable.iterator())

    inner class IteratorImp( val iterator: MutableIterator<From>) : Iterator<To> {
        var _next : To? = null
        var done = false

        override tailrec fun hasNext() : Boolean = when{
            done -> false
            _next != null -> true
            else -> {
                spin()
                hasNext()
            }
        }
        private fun spin() {
            while(!done && _next == null) {
                if( !iterator.hasNext()) {
                    done = true
                    continue
                }
                _next = mapping(iterator.next())
                if( _next == null) {
                    iterator.remove()
                }
            }
        }

        override fun next(): To {
            // Return and consume the _next
            spin()
            val next = _next
            this._next = null
            return next ?: throw IndexOutOfBoundsException()
        }

    }

}