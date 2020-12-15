package rb.animo.io.aafWriter

import rb.animo.animation.AafStructure
import rb.animo.io.IWriter

interface IAafWriter {
    fun write(writer: IWriter, aaf: AafStructure)
}

object Chunkifier {
    fun aggregateLikeChunks(aaf: AafStructure) =
            aaf.animations
                    .flatMap { it.frames  }
                    .flatMap { it.chunks }
                    .map { it.celRect }
                    .distinct()
                    .mapIndexed { index, rectI -> Pair(index, rectI) }
                    .toMap()
}

class AafWriter_v2 : IAafWriter {
    override fun write(writer: IWriter, aaf: AafStructure) {
//        writer.writeInt(2) // [4] Version number
//
//
//        writer.writeShort(aaf.animations.count()) // [2] NumAnimations
//        for ( anim in aaf.animations) {
//            writer.writeUtf8(anim.name) // [N] AnimName
//
//            writer.writeShort(anim.frames.count()) // [2] : NumFrames
//            for (frame in anim.frames) {
//                writer.writeShort(frame.chunks.count()) // [2] : NumChunks
//                for (chunk in frame.chunks) {
//                    writer.writeShort(chunk.)
//
//                }
//
//            }
//        }
    }

}