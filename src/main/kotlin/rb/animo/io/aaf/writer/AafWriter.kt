package rb.animo.io.aaf.writer

import rb.animo.io.aaf.AafFile
import rb.file.IFileWriter
import rb.file.writeUtf8
import rb.vectrix.mathUtil.i

interface IAafWriter {
    fun write(writer: IFileWriter, aafFile: AafFile)
}

class AafWriter_v2_to_v4(val version: Int) : IAafWriter{
    override fun write(writer: IFileWriter, aafFile: AafFile) {
        writer.writeInt(version)

        writer.writeShort(aafFile.animations.count()) // [2] : NumAnims
        for (anim in aafFile.animations) {
            writer.writeUtf8(anim.name) // [n] Name
            if( version >= 3) {
                writer.writeShort(anim.ox) // [2] OriginX
                writer.writeShort(anim.oy) // [2] OriginY
            }

            writer.writeShort(anim.frames.count()) // [2] : NumFrames
            for (frame in anim.frames) {
                if( version == 2)
                    writer.writeShort(frame.chunks.count())
                else
                    writer.writeByte(frame.chunks.count()) // [1] : Chunk Count
                for (chunk in frame.chunks) {
                    if( version >= 4)
                        writer.writeByte(chunk.group.toByte().i) // [1] : GroupId
                    writer.writeShort(chunk.celId) // [2] : CelId
                    writer.writeShort(chunk.offsetX) // [2] : OffsetX
                    writer.writeShort(chunk.offsetY) // [2] : OffsetY
                    writer.writeInt(chunk.drawDepth) // [4] : DrawDepth
                }

                if( version >= 3) {
                    writer.writeByte(frame.hitboxes.count()) // [1] Num Hitboxes
                    for ( hitbox in frame.hitboxes) {
                        writer.writeByte(hitbox.typeId) // [1] TypeId
                        AafColisionWriter.write(writer, hitbox.col) // [N] Collision Data
                    }
                }
            }
        }

        writer.writeShort(aafFile.cels.count()) // [2] : NumCels
        for(cel in aafFile.cels) {
            writer.writeShort(cel.x)
            writer.writeShort(cel.y)
            writer.writeShort(cel.w)
            writer.writeShort(cel.h)
        }
    }
}