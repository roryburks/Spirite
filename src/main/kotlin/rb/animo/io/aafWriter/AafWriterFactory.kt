package rb.animo.io.aafWriter

interface IAafWriterFactory {
    fun get(version: Int? = null) : IAafWriter
}

object AafWriterFactory : IAafWriterFactory {
    override fun get(version: Int?) = when(version ?: 2) {
        2 -> AafWriter_v2
        else -> throw NotImplementedError("unsupported Aaf Export Version: $version")
    }
}