package rb.animo.io.aafWriter

interface IAafWriterFactory {
    fun get(version: Int? = null) : IAafWriter
}

object AafWriterFactory : IAafWriterFactory {
    override fun get(version: Int?) = when( val v = version ?: 4) {
        2 -> AafWriter_v2
        3, 4 -> AafWriter_v3_to_4(v)
        else -> throw NotImplementedError("unsupported Aaf Export Version: $version")
    }
}