package rb.animo.io

interface ILoader<T> {
    fun load(string: String, onLoad: (T)->Unit, onFail: (Exception?) -> Unit)
}