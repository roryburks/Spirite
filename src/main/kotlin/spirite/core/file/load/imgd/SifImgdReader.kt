package spirite.core.file.load.imgd

import rb.file.IReadStream
import spirite.core.file.contracts.SifImgdChunk
import spirite.core.file.contracts.SifImgdMedium

class SifImgdReader(val version: Int) {
    fun read( read: IReadStream, endPtr: Long) : SifImgdChunk {
        val mediums = mutableListOf<SifImgdMedium>()
        while( read.filePointer < endPtr) {
            val mediumId = read.readInt()
            val typeId = if( version < 4) 0 else read.readUnsignedByte()
            val data = SifMediumReaderFactory.getMediumReader(version, typeId)
                .read(read)

            mediums.add(SifImgdMedium(mediumId, data))
        }

        return SifImgdChunk(mediums)
    }
}