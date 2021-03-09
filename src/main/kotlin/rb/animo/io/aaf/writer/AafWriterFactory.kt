package rb.animo.io.aaf.writer

interface  IAafWriterFactory {
    fun makeWriter(version: Int) : IAafWriter
}

object AafWriterFactory : IAafWriterFactory{
    override fun makeWriter(version: Int): IAafWriter {
        return AafWriter_v2_to_v4(version)
    }
}