package rb.extendo.extensions
fun <T> Iterator<T>.fillToList() : List<T> {
    val list = mutableListOf<T>()
    for (t in this) {
        list.add(t)
    }
    return list
}
