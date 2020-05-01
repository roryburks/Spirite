package rb.extendo.extensions


fun <T> MutableList<T>.pop() : T {
    val ret = get(lastIndex)
    removeAt(this.lastIndex)
    return ret
}

fun <T> MutableList<T>.popOrNull() : T? {
    val ret = lastOrNull()
    return ret?.also { removeAt(lastIndex) }
}

fun <T> MutableList<T>.mutate(index: Int, transform: (T)->T) {
    val change = this.getOrNull(index)?.let { transform(it) } ?: return
    this[index] = change
}

fun <T> MutableList<T>.replace(from: T, to:T) {
    val index = indexOf(from)
    if( index == -1) return
    this[index] = to
}

