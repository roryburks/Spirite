package spirite.core.file.save

import rb.file.IWriteStream
import spirite.core.file.SifConstants
import spirite.core.file.contracts.SifAnspChunk

object SifAnspWriter {
    const val MaxFfaAnims = Short.MAX_VALUE.toInt()
    const val MaxFfaLinks = Short.MAX_VALUE.toInt()

    fun write(out: IWriteStream, data: SifAnspChunk) {
        for (space in data.spaces) {
            out.writeStringUft8Nt(space.name)
            out.writeByte(SifConstants.ANIMSPACE_FFA)

            val anims = space.anims.take(MaxFfaAnims)
            out.writeShort(anims.size)
            for (anim in anims) {
                out.writeInt(anim.animId)
                out.writeInt(anim.endLinkAnimId)
                if( anim.endLinkAnimId != -1)
                    out.writeInt(anim.onEndFrame ?: 0)
                out.writeShort(anim.logicalX)
                out.writeShort(anim.logicalY)
            }

            val links = space.links.take(MaxFfaLinks)
            out.writeShort(links.size)
            for (link in links) {
                out.writeInt(link.origAnimId)
                out.writeInt(link.origFrame)
                out.writeInt(link.destAnimId)
                out.writeInt(link.destFrame)
            }
        }
    }
}