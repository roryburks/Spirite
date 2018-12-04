package rb.extendo.extensions

fun <T> MutableIterable<T>.removeToList(filter: (T)->Boolean) : MutableList<T>{
    val removed = mutableListOf<T>()
    val iter = iterator()
    while( iter.hasNext()) {
        val t = iter.next()
        if( filter(t)) {
            iter.remove()
            removed.add(t)
        }
    }
    return removed
}

fun <T> MutableIterable<T>.removeFirst(filter: (T) -> Boolean) : T? {
    val iter = iterator()
    while (iter.hasNext()) {
        val t = iter.next()
        if( filter(t)) {
            iter.remove()
            return t
        }
    }
    return null
}