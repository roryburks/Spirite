package rb.extendo.extensions

fun <T> Iterable<T>.asHashSet() =
        if( this is HashSet<T>) this
        else this.toHashSet()