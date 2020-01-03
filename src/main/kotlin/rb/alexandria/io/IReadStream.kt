package rb.alexandria.io

interface IReadStream {
    val pointer: Long
    fun goto(pointer: Long)
}