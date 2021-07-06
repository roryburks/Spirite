package spirite.core.file.load

import rb.file.IReadStream
import spirite.core.file.SifConstants
import spirite.core.file.SifFileException
import spirite.core.file.contracts.SifFile


object SifFileReader {
    fun read(read: IReadStream) : SifFile {
        // Top Level Info
        read.filePointer = 0
        val header = read.readByteArray(4)
        if(!header.contentEquals( SifConstants.header))
            throw SifFileException("Bad Fileheader (not an SIF File or corrupt)")

        val version = read.readInt()
        var width = 0
        var height = 0
        if( version >= 1){
            width = read.readUnsignedShort()
            height = read.readUnsignedShort()
        }

        val chunks = parseChunks(read)

        TODO()
    }

    data class ChunkInfo(
        val header: String,
        val startPointer: Long,
        val size: Int)

    fun parseChunks(read: IReadStream) : List<ChunkInfo> {
        TODO()
    }
}