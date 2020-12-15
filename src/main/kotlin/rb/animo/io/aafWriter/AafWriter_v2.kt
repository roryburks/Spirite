package rb.animo.io.aafWriter

import rb.animo.animation.AafStructure
import rb.animo.io.IWriter

object AafWriter_v2 : IAafWriter {
    override fun write(writer: IWriter, aaf: AafStructure) {
        writer.writeInt(2) // [4] Version number

        val chunked = Chunkifier.aggregateLikeChunks(aaf).toList()
        val chunkMap = chunked
                .mapIndexed { index, rectI ->  Pair(rectI, index) }
                .toMap()

        writer.writeShort(aaf.animations.count()) // [2] NumAnimations
        for ( anim in aaf.animations) {
            writer.writeUtf8(anim.name) // [N] AnimName

            writer.writeShort(anim.frames.count()) // [2] : NumFrames
            for (frame in anim.frames) {
                writer.writeShort(frame.chunks.count()) // [2] : NumChunks
                for (chunk in frame.chunks) {
                    val celid = chunkMap[chunk.celRect]!!
                    writer.writeShort(celid) // [2] UShort
                    writer.writeShort(chunk.offsetX) // [2] OffsetX
                    writer.writeShort(chunk.offsetY) // [2] OffsetY
                    writer.writeInt(chunk.drawDepth)
                }
            }
        }

        writer.writeShort(chunked.count()) // [2] NumCells
        for( cel in chunked) {
            writer.writeShort(cel.x1i)
            writer.writeShort(cel.y1i)
            writer.writeShort(cel.wi)
            writer.writeShort(cel.hi)
        }
    }
}