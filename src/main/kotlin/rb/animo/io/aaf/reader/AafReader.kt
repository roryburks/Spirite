package rb.animo.io.aaf.reader

import rb.animo.io.aaf.AafFile
import rb.file.IFileReader

interface IAafReader {
    fun read( reader: IFileReader) : AafFile
}

object AafReaderFactory {
    fun readVersionAndGetReader(reader: IFileReader) : IAafReader {
        val version = reader.readInt()
        if(version in 2..4)
            return AafReader_v2_to_4(version)
        throw NotImplementedError("Unsupported Version Number: $version")
    }
}

