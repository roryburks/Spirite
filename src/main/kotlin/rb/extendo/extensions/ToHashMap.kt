package rb.extendo.extensions


fun <T,Key> Collection<T>.toHashMap( selector: (T)->Key) : HashMap<Key,T> {
    val map = mutableMapOf<Key,T>()
    this.forEach {
        map[selector(it)] = it
    }
    return HashMap(map)
}

fun <T,Key,Out> Collection<T>.toHashMap( selector: (T)->Key, mutator:(T)->Out) : HashMap<Key,Out> {
    val map = mutableMapOf<Key,Out>()
    this.forEach {
        map[selector(it)] = mutator(it)
    }
    return HashMap(map)
}