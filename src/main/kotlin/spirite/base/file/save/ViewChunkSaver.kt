package spirite.base.file.save

import rb.glow.gle.RenderMethodType

object ViewChunkSaver {
    fun saveViewChunk(context: SaveContext) {
        val reverseNodeMap = context.nodeMap
            .map { Pair(it.value, it.key) }
            .sortedBy { it.first }
            .toList()
        val viewSystem = context.workspace.viewSystem
        context.writeChunk("VIEW") {ra ->
            val numViews = viewSystem.numActiveViews

            // [1] Num Views
            ra.writeByte(numViews)

            repeat(numViews) { viewNum ->
                val selected = context.nodeMap[viewSystem.getCurrentNode(viewNum)] ?: -1
                ra.writeInt(selected)
                for ((_,node) in reverseNodeMap) {
                    val nodeProps = viewSystem.get(node, viewNum)

                    // [1] Property Bitmap
                    val bitMap = if( nodeProps.isVisible) 1 else 0
                    ra.writeByte(bitMap)
                    // [4] Alpha
                    ra.writeFloat(nodeProps.alpha)
                    // [1] Render Method
                    ra.writeInt(nodeProps.method.methodType.ordinal)
                    // [4] Render Value
                    ra.writeInt(nodeProps.method.renderValue)
                    // [2] Ox
                    ra.writeShort(nodeProps.ox)
                    // [2] Oy
                    ra.writeShort(nodeProps.oy)
                }

            }
        }

    }
}