package rb.animo.io.aafReader

interface IAafReaderFactory  {
    fun getReader(version: Int) : IAafReader
}

object AafReaderFactory : IAafReaderFactory {
    override fun getReader(version: Int) = when( version) {
        2 -> AafReader_v2
        3, 4 -> AafReader_v3_to_4(version)
        else -> throw NotImplementedError("Unsupported Aaf Version: $version")
    }
}