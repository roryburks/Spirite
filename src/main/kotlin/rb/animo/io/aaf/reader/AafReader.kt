package rb.animo.io.aaf.reader

import rb.animo.io.IReader
import rb.animo.io.aaf.AafFile

interface IAafReader {
    fun read( reader: IReader) : AafFile
}

object AafReaderFactory {
    fun readVersionAndGetReader(reader: IReader) : IAafReader {
        val version = reader.readInt()
        if(version in 2..4)
            return AafReader_v2_to_4(version)
        throw NotImplementedError("Unsupported Version Number: $version")
    }
}

