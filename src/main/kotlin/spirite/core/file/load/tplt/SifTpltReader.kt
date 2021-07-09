package spirite.core.file.load.tplt

import rb.file.IReadStream
import rb.file.readStringUtf8
import spirite.core.file.contracts.SifTpltChunk
import spirite.core.file.contracts.SifTpltNodeMap
import spirite.core.file.contracts.SifTpltSpritePartMap

class SifTpltReader(val version: Int) {
    fun read( read: IReadStream, endPtr: Long) : SifTpltChunk {
        val numMappedNodes = read.readInt()
        val nodeMaps = List(numMappedNodes) {
            val nodeId = read.readInt()
            val colorSize = read.readUnsignedByte()
            val colors = List(colorSize){ read.readInt() } //NOTE: Maybe eventually need ReadIntArray

            SifTpltNodeMap(nodeId, colors)
        }

        val numSpriteMaps = read.readInt()
        val spriteMap = List(numSpriteMaps){
            val groupId = read.readInt()
            val partName = read.readStringUtf8()
            val colorSize = read.readUnsignedByte()
            val colors = List(colorSize){ read.readInt() } //NOTE: Maybe eventually need ReadIntArray

            SifTpltSpritePartMap(groupId, partName, colors)
        }

        return SifTpltChunk(nodeMaps, spriteMap)
    }
}