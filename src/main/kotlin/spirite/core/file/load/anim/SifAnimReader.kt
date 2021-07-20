package spirite.core.file.load.anim

import rb.file.IReadStream
import rb.file.readStringUtf8
import rb.vectrix.mathUtil.i
import spirite.core.file.contracts.SifAnimAnimation
import spirite.core.file.contracts.SifAnimChunk

class SifAnimReader(val version: Int) {
    fun read(read: IReadStream, endpointer: Long) : SifAnimChunk {
        val animations = mutableListOf<SifAnimAnimation>()

        while( read.filePointer < endpointer) {
            val name = if( version == 0x1_0000 ) "animation" else read.readStringUtf8()
            val speed = if( version < 0x1_0005) 8f else read.readFloat()
            val zoom = if( version <0x1_000C) 1 else read.readShort()
            val type = read.readByte().i

            val dataLoader = SifAnimAnimationReaderFactory.getReader(type, version)
            val data = dataLoader.read(read)

            animations.add(SifAnimAnimation( name, speed, zoom, data ))
        }

        return SifAnimChunk(animations)
    }
}