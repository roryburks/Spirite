package rb.animo.io.aaf.reader

import rb.animo.io.aaf.*
import rb.file.IFileReader
import rb.file.readUtf8
import rb.vectrix.mathUtil.i

class AafReader_v2_to_4( val version: Int) : IAafReader {
    override fun read(reader: IFileReader): AafFile {
        val numAnims = reader.readUnsignedShort()

        val anims = List(numAnims) {
            val animName = reader.readUtf8()

            // Read Offset
            val ox: Int
            val oy: Int
            if( version == 2) {
                ox = 0
                oy = 0
            }
            else {
                ox = reader.readShort().i
                oy = reader.readShort().i
            }


            val numFrames = reader.readUnsignedShort()
            val frames = List(numFrames) {

                val numChunks = if( version == 2) reader.readUnsignedShort() else reader.readUnsignedByte()

                val chunks = List(numChunks){
                    AafFChunk(
                        group = if( version >= 4) reader.readByte().toChar() else ' ',
                        celId = reader.readUnsignedShort(),
                        offsetX = reader.readShort().i,
                        offsetY = reader.readShort().i,
                        drawDepth = reader.readInt() )
                }

                val hitboxes : List<AafFHitbox>
                if( version< 2)
                    hitboxes = emptyList<AafFHitbox>()
                else {
                    val numHitboxes = reader.readUnsignedByte()
                    hitboxes = List(numHitboxes) {
                        AafFHitbox(
                            typeId = reader.readUnsignedByte(),
                            col = AafColisionReader.read(reader) )
                    }
                }

                AafFFrame(
                    chunks = chunks,
                    hitboxes = hitboxes )
            }
            AafFAnimation(
                animName,
                ox, oy,
                frames)
        }

        // Cel
        val numCels = reader.readUnsignedShort()
        val cels = List(numCels) {
            AafFCel(
                x = reader.readShort().i,
                y = reader.readShort().i,
                w = reader.readUnsignedShort(),
                h = reader.readUnsignedShort())
        }

        return AafFile(
            version,
            animations =anims,
            cels = cels )
    }

}