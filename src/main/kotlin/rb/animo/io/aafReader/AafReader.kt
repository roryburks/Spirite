package rb.animo.io.aafReader

import rb.animo.animation.*
import rb.animo.io.IReader
import rb.vectrix.mathUtil.i
import rb.vectrix.shapes.RectI

interface IAafReader {
    fun read( reader: IReader) : AafStructure
}

internal class IntAnim(
    val name: String,
    val frames: List<IntFrame>,
    val ox: Int = 0,
    val oy: Int = 0)
internal class IntFrame(
    val chunks: List<IntChunk>,
    val hboxes: List<AafHitbox> = emptyList())
internal class IntChunk(
    val celId: Int,
    val offsetX: Short,
    val offsetY: Short,
    val drawDepth: Int,
    val cid: Char)

object AafReader_v2 : IAafReader {
    override fun read(data: IReader): AafStructure {
        val numAnims = data.readUShort()

        val anims = List(numAnims) {
            val animName = data.readUtf8()
            val numFrames = data.readUShort()
            val frames = List(numFrames) {
                val numChunks = data.readUShort()

                val chunks = List(numChunks){
                    IntChunk(
                            celId = data.readUShort(),
                            offsetX = data.readShort(),
                            offsetY = data.readShort(),
                            drawDepth = data.readInt(),
                            cid = ' ')
                }
                IntFrame(chunks)
            }
            IntAnim(animName, frames)
        }

        // Cel
        val numCels = data.readUShort()
        val cels = List(numCels) {
            val x = data.readShort()
            val y = data.readShort()
            val w = data.readUShort()
            val h = data.readUShort()
            RectI(x.i, y.i, w, h)
        }

        // Spac (ignored)

        // Build Animations
        val aafAnims = anims
                .map {
                    AafAnimStructure(it.name, it.frames.map { frame ->
                        AafFrame(frame.chunks.map { chunk ->
                            AafChunk(
                                    cels[chunk.celId],
                                    chunk.offsetX.i,
                                    chunk.offsetY.i,
                                    chunk.drawDepth
                            )
                        })
                    })
                }

        return AafStructure(aafAnims)
    }


}