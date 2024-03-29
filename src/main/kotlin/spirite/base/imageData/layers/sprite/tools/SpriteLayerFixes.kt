package spirite.base.imageData.layers.sprite.tools

import rb.glow.Color
import rb.vectrix.mathUtil.MathUtil
import spirite.base.brains.toolset.ColorChangeMode
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.groupTree.GroupNode
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.groupTree.Node
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.mediums.magLev.MaglevColorChangeModule
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.imageData.mediums.magLev.util.MaglevConverter

object SpriteLayerFixes {
    fun CycleParts(group: GroupNode, partNames: List<String>, met: Int, ws: IImageWorkspace){
        ws.undoEngine.doAsAggregateAction("Cycle Sprite Parts $met")
        {
            val pnFilter = partNames.toHashSet()
            val spriteLayers = group.children
                    .filterIsInstance<LayerNode>()
                    .map { it.layer }
                    .filterIsInstance<SpriteLayer>()

            val ct = spriteLayers.count()

            val storedPartStructures = spriteLayers
                    .map { spriteLayer ->
                        spriteLayer.parts.filter { pnFilter.contains(it.partName) }
                                .map { Pair(it.structure, it.handle) }
                    }

            spriteLayers.forEach { spriteLayer ->
                spriteLayer.parts
                        .filter { pnFilter.contains(it.partName) }
                        .forEach { spriteLayer.removePart(it) }
            }

            (0 until ct).forEach { i ->
                val ni = MathUtil.cycle(0, ct, i+met)
                storedPartStructures[i].forEach { (structure, handle) ->
                    spriteLayers[ni].insertPart(handle, structure)
                }
            }
        }

    }

    fun SpriteMaglevToDynamic(sprite: SpriteLayer) {
        val ws = sprite.workspace
        ws.undoEngine.doAsAggregateAction("Flatten Maglevs to Sptires")
        {
            val puppetParts = sprite.parts
                    .filter { it.handle.medium is MaglevMedium }

            val newStructure = puppetParts.map {
                val maglev = it.handle.medium as MaglevMedium
                val dynamic = MaglevConverter.convertToDynamic(maglev, ws)
                val handle = ws.mediumRepository.addMedium(dynamic)
                Pair(handle, it.structure)
            }

            newStructure.forEach { (handle, structure) -> sprite.insertPart(handle, structure) }
            puppetParts.forEach { sprite.removePart(it) }
        }
    }

    fun colorChangeEntireNodeContext(node: Node, from: Color, to: Color, mode: ColorChangeMode, ws: IImageWorkspace) {
        val maglevMediums = node.getLayerNodes()
                .flatMap { it.layer.allArrangedData }
                .filter { it.handle.medium is MaglevMedium }

        if( maglevMediums.any())
        {
            ws.undoEngine.doAsAggregateAction("Color Change Entire Node Context") {
                maglevMediums.forEach { arranged ->
                    ws.undoEngine.performAndStore(MaglevColorChangeModule.MaglevColorChangeAction( arranged, from, to, mode ))
                }
            }
        }
    }


}