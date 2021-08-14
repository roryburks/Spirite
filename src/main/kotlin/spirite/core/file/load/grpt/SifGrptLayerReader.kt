package spirite.core.file.load.grpt

import rb.file.IReadStream
import rb.file.readStringUtf8
import rb.vectrix.mathUtil.i
import spirite.core.file.SifConstants
import spirite.core.file.SifFileException
import spirite.core.file.contracts.SifGrptNodeData
import spirite.core.file.contracts.SifGrptNode_Reference
import spirite.core.file.contracts.SifGrptNode_Simple
import spirite.core.file.contracts.SifGrptNode_Sprite

interface ISifGrptLayerReader {
    fun readLayer(read: IReadStream) : SifGrptNodeData
}

object SifGrptLayerReaderFactory {
    fun getLoader(version: Int, typeId: Int) : ISifGrptLayerReader{
        return when( typeId) {
            SifConstants.NODE_SIMPLE_LAYER -> SimpleLayerReader(version)
            SifConstants.NODE_SPRITE_LAYER -> when{
                version <= 4 -> LegacySpriteLayerReader(version)
                else -> SpriteLayerReader(version)
            }
            SifConstants.NODE_REFERENCE_LAYER -> IgnoreReferenceLayerReader
            SifConstants.NODE_PUPPET_LAYER -> IgnorePuppetLayerReader
            else -> throw SifFileException("Unrecognized GroupNode Type ID: $typeId (version mismatch or corrupt file?)")
        }
    }
}

class SimpleLayerReader(val version: Int) : ISifGrptLayerReader {
    override fun readLayer(read: IReadStream): SifGrptNodeData {
        return SifGrptNode_Simple(read.readInt())
    }
}

class LegacySpriteLayerReader(val version: Int) : ISifGrptLayerReader {
    override fun readLayer(read: IReadStream): SifGrptNodeData {
        val numParts = read.readUnsignedByte()
        val parts = List<SifGrptNode_Sprite.Part>(numParts) {
            val partName = read.readStringUtf8()
            val transX = read.readShort().toFloat()
            val transY = read.readShort().toFloat()
            val drawDepth = read.readInt()
            val mediumId = read.readInt()

            SifGrptNode_Sprite.Part(
                partName,
                transX,
                transY,
                1f,
                1f,
                0f,
                drawDepth,
                mediumId,
                1f )
        }

        return SifGrptNode_Sprite(
            SifConstants.MEDIUM_DYNAMIC,
            parts)
    }
}

class SpriteLayerReader(val version: Int) : ISifGrptLayerReader {
    override fun readLayer(read: IReadStream): SifGrptNodeData {
        val type = if(version >= 0x1_000A) read.readByte().i
            else SifConstants.MEDIUM_DYNAMIC
        val numParts = read.readUnsignedByte()
        val parts = List(numParts){
            val name = read.readStringUtf8()
            val transX = read.readFloat()
            val transY = read.readFloat()
            val scaleX = read.readFloat()
            val scaleY = read.readFloat()
            val rot = read.readFloat()
            val drawDepth = read.readInt()
            val mediumId = read.readInt()
            val alpha = if( version >= 0x1_0003) read.readFloat() else 1f

            SifGrptNode_Sprite.Part(name, transX, transY, scaleX, scaleY, rot, drawDepth, mediumId, alpha)
        }

        return SifGrptNode_Sprite(
            type,
            parts )
    }
}

object IgnoreReferenceLayerReader : ISifGrptLayerReader {
    override fun readLayer(read: IReadStream): SifGrptNodeData {
        val nodeId = read.readInt() // [4] NodeId
        return SifGrptNode_Reference(nodeId)
    }
}

object IgnorePuppetLayerReader : ISifGrptLayerReader {
    override fun readLayer(read: IReadStream): SifGrptNodeData {
        val byte = read.readByte()   // [1] : Whether or not is derived
        if( byte.i == 0) {
            val numParts = read.readUnsignedShort() // [2] : Number of parts
            for( i in 0 until numParts) {
                read.readShort()  // [2] : Parent
                read.readInt()    // [4] : MediumId
                read.readFloat()  // [16] : Bone x1, y1, x2, y2
                read.readFloat()
                read.readFloat()
                read.readFloat()
                read.readInt()    // [4]: DrawDepth
            }
            return SifGrptNode_Reference(-1)
        }
        else throw SifFileException("Do not know how to handle derived puppet types")
    }
}