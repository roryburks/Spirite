package rb.extendo.extensions

fun <A,B,C> Map<A,B>.nest(other : Map<B,C>) : Map<A,C> {
    val out = HashMap<A,C>(this.size)

    this.forEach { t, u -> out[t] = other[u]!! }

    return out
}