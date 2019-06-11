package rb.glow

interface IFlushable {
    fun flush()
}

fun <T, F> using(raw: F, doer: (F)->T) : T where  F : IFlushable {
    try {
        return doer(raw)
    }finally {
        raw.flush()
    }
}

fun <T, F> F.with(doer: (F)->T) : T where  F : IFlushable {
    try {
        return doer(this)
    }finally {
        this.flush()
    }
}