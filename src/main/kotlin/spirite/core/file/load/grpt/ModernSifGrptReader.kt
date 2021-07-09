package spirite.core.file.load.grpt

import rb.file.IReadStream
import rb.file.readStringUtf8
import rb.vectrix.mathUtil.i
import spirite.core.file.SifConstants
import spirite.core.file.SifFileException
import spirite.core.file.contracts.SifGrptChunk
import spirite.core.file.contracts.SifGrptNode
import spirite.core.file.contracts.SifGrptNodeData
import spirite.core.file.contracts.SifGrptNode_Group

class ModernSifGrptReader(val version: Int) : ISifGrptReader {
    init {
        if( version <= 1)
            throw SifFileException("Unsupported Version Number: $version")
    }

    override fun read(read: IReadStream, endPointer: Long) : SifGrptChunk {
        val nodes = mutableListOf<SifGrptNode>()
        while( read.filePointer < endPointer) {
            val depth = read.readByte()

            var alpha: Float? = null
            var oX: Int? = null
            var oY: Int? = null
            if( version < 0x0001_0010) {
                alpha = read.readFloat()
                oX = read.readShort().i
                oY = read.readShort().i
            }

            val bitFlag = read.readByte()
            val name = read.readStringUtf8()
            val type = read.readByte().i

            val data : SifGrptNodeData = when( type) {
                SifConstants.NODE_GROUP -> SifGrptNode_Group
                else -> SifGrptLayerReaderFactory.getLoader(version, type).readLayer(read)
            }

            nodes.add(SifGrptNode(bitFlag, name, data, depth.i, alpha, oX, oY))
        }

        return SifGrptChunk(nodes)
    }
}