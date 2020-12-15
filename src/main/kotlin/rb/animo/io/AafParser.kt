package rb.animo.io

import rb.animo.animation.*
import rb.vectrix.intersect.*
import rb.vectrix.mathUtil.d
import rb.vectrix.mathUtil.i
import rb.vectrix.shapes.CircleD
import rb.vectrix.shapes.LineSegmentD
import rb.vectrix.shapes.RectD
import rb.vectrix.shapes.RectI

object AafParser {
    private class IntAnim(
        val name: String,
        val frames: List<IntFrame>,
        val ox: Int = 0,
        val oy: Int = 0)
    private class IntFrame(
        val chunks: List<IntChunk>,
        val hboxes: List<AafHitbox> = emptyList())
    private class IntChunk(
        val celId: Int,
        val offsetX: Short,
        val offsetY: Short,
        val drawDepth: Int)

    fun parseAaf(data: IReader) : AafStructure {

        // Animations
        val version = data.readInt()

        return when(version) {
            2 -> v2Loader(data)
            3 -> v3Loader(data)
            else -> throw NotImplementedError()
        }
    }

    fun v2Loader(data: IReader) : AafStructure {
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
                        drawDepth = data.readInt())
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

    fun v3Loader(data: IReader) : AafStructure {
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
                    IntChunk(
                        celId = data.readUShort(),
                        offsetX = data.readShort(),
                        offsetY = data.readShort(),
                        drawDepth = data.readInt())
                }

                val numHbox = data.readByte()
                val hitboxes  = List(numHbox) {
                    AafHitbox(typeId = data.readByte(), col = loadHitbox(data))
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
                            chunk.drawDepth
                        )
                    }, frame.hboxes)
                }, anim.ox, anim.oy)
            }

        return AafStructure(aafAnims)
    }

    fun loadHitbox(data: IReader) : CollisionObject {
        return when( val colTypeId = data.readByte()) {
            FileConsts.ColKind_Point -> CollisionPoint(
                data.readFloat().d,
                data.readFloat().d
            )
            FileConsts.ColKind_RigidRect -> CollisionRigidRect(
                RectD(
                    data.readFloat().d,
                    data.readFloat().d,
                    data.readFloat().d,
                    data.readFloat().d
                )
            )
            FileConsts.ColKind_Circle -> CollisionCircle(
                CircleD.Make(
                    data.readFloat().d,
                    data.readFloat().d,
                    data.readFloat().d
                )
            )
            FileConsts.ColKind_LineSegment -> CollisionLineSegment(
                LineSegmentD(
                    data.readFloat().d,
                    data.readFloat().d,
                    data.readFloat().d,
                    data.readFloat().d
                )
            )
            else -> throw NotImplementedError("Unrecognized Collision Type: $colTypeId")
        }
    }
}