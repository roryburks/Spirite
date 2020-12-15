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
}

