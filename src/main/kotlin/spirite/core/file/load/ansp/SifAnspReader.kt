package spirite.core.file.load.ansp

import rb.file.IReadStream
import rb.file.readStringUtf8
import spirite.core.file.SifConstants
import spirite.core.file.SifFileException
import spirite.core.file.contracts.SifAnspAnim
import spirite.core.file.contracts.SifAnspChunk
import spirite.core.file.contracts.SifAnspLink
import spirite.core.file.contracts.SifAnspSpace

class SifAnspReader(val version: Int) {
    fun read(read: IReadStream, endPtr: Long) : SifAnspChunk {
        val spaces = mutableListOf<SifAnspSpace>()
        while( read.filePointer < endPtr) {
            val name = read.readStringUtf8()
            val type = read.readUnsignedByte()

            if( type != SifConstants.ANIMSPACE_FFA)
                throw SifFileException("Unrecognized AnimationSpace Type ID: $type (version mismatch or corrupt file?)")

            val numAnims = read.readUnsignedShort()
            val anims = List(numAnims){
                val animId = read.readInt()

                val endLinkAnimId = read.readInt()
                val endLinkFrame = if( endLinkAnimId == -1) null else read.readInt()

                val logX = read.readUnsignedShort()
                val logY = read.readUnsignedShort()

                SifAnspAnim(animId, endLinkAnimId, endLinkFrame, logX, logY)
            }

            val numLinks = read.readUnsignedShort()
            val links = List(numLinks) {
                val origAnimId = read.readInt()
                val origFrame = read.readInt()
                val destAnimId = read.readInt()
                val destFrame = read.readInt()

                SifAnspLink(origAnimId, origFrame, destAnimId, destFrame)
            }

            spaces.add(SifAnspSpace(name, anims, links))
        }

        return SifAnspChunk(spaces)
    }
}