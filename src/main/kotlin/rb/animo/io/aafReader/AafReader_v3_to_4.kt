package rb.animo.io.aafReader

import rb.animo.animation.*
import rb.animo.io.IReader
import rb.vectrix.mathUtil.i
import rb.vectrix.shapes.RectI

class AafReader_v3_to_4 (val version: Int) : IAafReader {
    override fun read(data: IReader): AafStructure {
        println("v3 Loader")
        val numAnims = data.readUShort()

        val anims = List(numAnims) {
            val animName = data.readUtf8()
            val ox = data.readShort()
            val oy = data.readShort()
            val numFrames = data.readUShort()
            val frames = List(numFrames) {
                val numChunks = data.readByte()

                val chunks = List(numChunks){
                    val cid = if( version >= 4) data.readByte().toChar() else ' '
                    IntChunk(
                            celId = data.readUShort(),
                            offsetX = data.readShort(),
                            offsetY = data.readShort(),
                            drawDepth = data.readInt(),
                            cid = cid)
                }

                val numHbox = data.readByte()
                val hitboxes  = List(numHbox) {
                    AafHitbox(typeId = data.readByte(), col = HitboxReader.loadHitbox(data))
                }
                IntFrame(chunks, hitboxes)
            }
            IntAnim(animName, frames, ox.i, oy.i)
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
                .map { anim ->
                    AafAnimStructure(anim.name, anim.frames.map { frame ->
                        AafFrame(frame.chunks.map { chunk ->
                            AafChunk(
                                    cels[chunk.celId],
                                    chunk.offsetX.i,
                                    chunk.offsetY.i,
                                    chunk.drawDepth,
                                    chunk.cid
                            )
                        }, frame.hboxes)
                    }, anim.ox, anim.oy)
                }

        return AafStructure(aafAnims)
    }

}