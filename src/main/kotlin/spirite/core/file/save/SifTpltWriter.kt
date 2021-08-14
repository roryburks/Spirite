package spirite.core.file.save

import rb.file.IWriteStream
import spirite.core.file.contracts.SifTpltChunk

object SifTpltWriter {
    const val MaxColors = 255

    fun write(out: IWriteStream, chunk: SifTpltChunk) {
        out.writeInt(chunk.nodeMaps.size)
        for (nodeMap in chunk.nodeMaps) {
            out.writeInt(nodeMap.nodeId)

            val colors = nodeMap.belt.take(MaxColors)
            out.writeByte(colors.size)
            colors.forEach { out.writeInt(it) }
        }

        out.writeInt(chunk.spritePartMaps.size)
        for (spritePartMap in chunk.spritePartMaps) {
            out.writeInt(spritePartMap.groupNodeId)
            out.writeStringUft8Nt(spritePartMap.partName)

            val colors = spritePartMap.belt.take(MaxColors)
            out.writeByte(colors.size)
            colors.forEach { out.writeInt(it) }
        }
    }
}