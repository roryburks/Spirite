package spirite.core.file.load.pltt

import rb.file.IReadStream
import rb.file.readStringUtf8
import spirite.core.file.contracts.SifPlttChunk
import spirite.core.file.contracts.SifPlttPalette

class SifPlttReader(val version: Int) {
    fun read( read: IReadStream, endPtr: Long) : SifPlttChunk {
        val palettes = mutableListOf<SifPlttPalette>()

        while (read.filePointer < endPtr) {
            val name = read.readStringUtf8()
            val size = read.readUnsignedShort()
            val raw = read.readByteArray(size)
            palettes.add(SifPlttPalette(name, raw))
        }

        return SifPlttChunk(palettes)
    }
}