package rb.extendo.extensions

fun <T> List<T>.stride(n: Int) : List<T> =
    (this.indices).step(n).map { this[it] }
