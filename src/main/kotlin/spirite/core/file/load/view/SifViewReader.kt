package spirite.core.file.load.view

import rb.file.IReadStream
import rb.vectrix.mathUtil.i
import spirite.core.file.contracts.SifGrptChunk
import spirite.core.file.contracts.SifViewChunk
import spirite.core.file.contracts.SifViewView

class SifViewReader(val version: Int) {
    fun read(read:IReadStream, endPtr: Long, grptChunk: SifGrptChunk) : SifViewChunk {
        val numNodes = grptChunk.nodes.size

        val numViews = read.readUnsignedByte()
        val views = List(numViews) {
            val selectedId = read.readInt()
            val nodeProps = List(numNodes){
                val bitMap = read.readByte()
                val alpha = read.readFloat()
                val renderMethod = read.readInt()
                val renderValue = read.readInt()
                val oX = read.readShort().i
                val oY = read.readShort().i

                SifViewView.Properties(bitMap, alpha, renderMethod, renderValue, oX, oY)
            }

            SifViewView(selectedId, nodeProps)
        }

        return SifViewChunk(views)
    }
}