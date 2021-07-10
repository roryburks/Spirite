package spirite.base.file.sif.v1.save

import spirite.base.file.writeUFT8NT
import kotlin.math.min

object PaletteMapSaver {
    fun savePaletteData( context: SaveContext) {

        context.writeChunk("TPLT") {ra ->
            val nodeMappings = context.workspace.paletteMediumMap.getNodeMappings()
            val spriteMappings = context.workspace.paletteMediumMap.getSpriteMappings()

            ra.writeInt(nodeMappings.size)  // [4] Number of Mapped Nodes
            nodeMappings.forEach { (node, colors) ->
                ra.writeInt(context.nodeMap[node] ?: -1)    // [4] : Node ID
                ra.writeByte(min(colors.size, 255))         // [1] : Size of Belt
                colors.take(255).forEach { ra.writeInt(it.argb32) } // [4] : Color ARGB
            }

            ra.writeInt(spriteMappings.size)    // [4] : Number of Mapped Sprite Parts
            spriteMappings.forEach { (pair, colors) ->
                val (node, spritePartName) = pair
                ra.writeInt(context.nodeMap[node] ?: -1)    // [4]: Node Id
                ra.writeUFT8NT(spritePartName)                  // [n] : Sprite Part Name
                ra.writeByte(min(colors.size, 255))         // [1] : Size of Belt
                colors.take(255).forEach { ra.writeInt(it.argb32) } // [4] : Color ARGB
            }
        }
    }
}