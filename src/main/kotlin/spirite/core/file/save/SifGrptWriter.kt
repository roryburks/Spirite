package spirite.core.file.save

import rb.file.IWriteStream
import rb.vectrix.mathUtil.i
import spirite.core.file.SifConstants
import spirite.core.file.SifFileException
import spirite.core.file.contracts.SifGrptChunk
import spirite.core.file.contracts.SifGrptNode_Group
import spirite.core.file.contracts.SifGrptNode_Sprite
import spirite.core.file.contracts.SifGrptNode_Simple

object SifGrptWriter {
    fun write(out: IWriteStream, data: SifGrptChunk) {
        data.nodes.forEach { node->
            out.writeByte(node.depth)
            out.writeByte(node.settingsBitFlag.i)
            out.writeStringUft8Nt(node.name)

            when( node.data) {
                is SifGrptNode_Group -> out.writeByte(SifConstants.NODE_GROUP)
                is SifGrptNode_Simple -> {
                    out.writeByte(SifConstants.NODE_SIMPLE_LAYER)
                    out.writeInt(node.data.mediumId)
                }
                is SifGrptNode_Sprite -> {
                    out.writeByte(SifConstants.NODE_SPRITE_LAYER)
                    out.writeByte(node.data.layerType)

                    val parts = node.data.parts.take(255)
                    if( node.data.parts.size > 255)
                        println("Trimmed Parts")

                    out.writeByte(parts.size)
                    for (part in parts) {
                        out.writeStringUft8Nt(part.partTypeName)
                        out.writeFloat(part.transX)
                        out.writeFloat(part.transY)
                        out.writeFloat(part.scaleX)
                        out.writeFloat(part.scaleY)
                        out.writeFloat(part.rotation)
                        out.writeInt(part.drawDepth)
                        out.writeInt(part.mediumId)
                        out.writeFloat(part.alpha)
                    }
                }
                else -> throw SifFileException("Unexpected Grpt Node Type")
            }
        }
    }
}