package sgui.transfer

interface ITransferObject {
    val dataTypes: Set<String>
    fun getData(type: String) : Any?
}


class StringTransferObject(val string: String) : ITransferObject {
    override val dataTypes: Set<String> get() = setOf(Key)

    override fun getData(type: String): Any? {
        return if( type == Key) string
        else null
    }

    companion object {
        const val Key = "String"
    }
}