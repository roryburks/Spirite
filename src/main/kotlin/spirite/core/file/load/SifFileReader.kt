package spirite.core.file.load

import rb.extendo.extensions.toLookup
import rb.file.IReadStream
import rb.vectrix.mathUtil.l
import spirite.core.file.SifConstants
import spirite.core.file.SifFileException
import spirite.core.file.contracts.*
import spirite.core.file.load.anim.SifAnimReader
import spirite.core.file.load.ansp.SifAnspReader
import spirite.core.file.load.grpt.ModernSifGrptReader
import spirite.core.file.load.imgd.SifImgdReader
import spirite.core.file.load.pltt.SifPlttReader
import spirite.core.file.load.tplt.SifTpltReader
import spirite.core.file.load.view.SifViewReader
import java.nio.charset.Charset


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

        val chunks = parseChunks(read).toLookup { it.header }

        fun <T> runOnChunk( headerName: String, lambda: (ChunkInfo)->T) : T? {
            val chunk = chunks[headerName]?.firstOrNull() ?: return null
            read.filePointer = chunk.startPointer
            return lambda(chunk)
        }

        val imgd = runOnChunk("IMGD"){ SifImgdReader(version).read(read, it.endPointer)}
            ?: SifImgdChunk(listOf())
        val grpt = runOnChunk("GRPT"){ ModernSifGrptReader(version).read(read, it.endPointer)}
            ?: SifGrptChunk(listOf())
        val anim = runOnChunk("ANIM"){ SifAnimReader(version).read(read, it.endPointer ) }
            ?: SifAnimChunk(listOf())
        val ansp = runOnChunk("ANSP"){SifAnspReader(version).read(read, it.endPointer)}
            ?: SifAnspChunk(listOf())
        val pltt = runOnChunk("PLTT"){SifPlttReader(version).read(read, it.endPointer)}
            ?: SifPlttChunk(listOf())
        val tplt = runOnChunk("TPLT"){SifTpltReader(version).read(read, it.endPointer)}
            ?: SifTpltChunk(listOf(), listOf())
        val view = runOnChunk("VIEW"){SifViewReader(version).read(read, it.endPointer, grpt)}
            ?: SifViewChunk(listOf())

        return SifFile(
            width, height, version,
            grpt, imgd, anim, pltt, tplt, ansp, view )
    }

    data class ChunkInfo(
        val header: String,
        val startPointer: Long,
        val size: Int)
    {
        val endPointer get() = startPointer + size
    }

    fun parseChunks(read: IReadStream) : List<ChunkInfo> {
        val chunks = mutableListOf<ChunkInfo>()
        while( !read.eof){
            val headerRaw = read.readByteArray(4)
            val header = headerRaw.toString(Charset.forName("UTF-8"))
            if(!SifConstants.chunks.contains(header))
                return chunks
                //throw SifFileException("Failed to parse chunks.  Unrecognized Chunk: $header")
            val size = read.readInt()
            val startPointer = read.filePointer

            chunks.add(ChunkInfo(header, startPointer, size))
            read.filePointer = read.filePointer + size.l
        }
        return  chunks
    }
}