package spirite.base.brains.commands.specific

import rb.extendo.dataStructures.SinglyList
import rb.glow.Color
import rb.vectrix.mathUtil.MathUtil
import spirite.base.brains.toolset.ColorChangeMode
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.groupTree.GroupTree
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.mediums.magLev.MaglevColorChangeModule
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.imageData.mediums.magLev.util.MaglevConverter

object SpriteLayerFixes {
    fun CycleParts(group: GroupTree.GroupNode, partNames: List<String>,  met: Int, ws: IImageWorkspace){
        ws.undoEngine.doAsAggregateAction("Cycle Sprite Parts $met")
        {
            val pnFilter = partNames.toHashSet()
            val spriteLayers = group.children
                    .filterIsInstance<GroupTree.LayerNode>()
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

    fun colorChangeEntireNodeContext(node: GroupTree.Node, from: Color, to: Color, mode: ColorChangeMode, ws: IImageWorkspace) {
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

    /***
     * Normalizes the Sprite Layers in the group tree node so that they have a common and consistent format.
     * Behavior:
     * 1. Selects the PrioritySpriteLayer, this one will be the final say of what depth ordering to use for common parts
     * 2. Create a Canonical part map PartName->String.  Initialize it with the layer structure of the PSL
     * 3. Using a greedy algorithm, for each part name not in the Canonical part map, find its first instance in the Sprite
     *    part, finding the part prior to it (if it exists), and create a Mapping This->Previous.  If Previous is in the
     *    canonical map, add this after the previous (while incrementing ordinals as necessary).  If not, recursively act
     *    on Previous until one is found.  (note previous can be null at which point canonicalmap.first - 1 is used)
     */
    fun normalizeSpriteLayers( node: GroupTree.Node): SuccessResponse  {
        val group = if( node is GroupTree.GroupNode) node else node.parent ?: return SuccessResponse.Error("No Group Found")
        val spriteLayers = group.children
            .filterIsInstance<GroupTree.LayerNode>()
            .map { it.layer }
            .filterIsInstance<SpriteLayer>()

        val prioritySpriteLayer = when (node) {
            is GroupTree.GroupNode -> spriteLayers.firstOrNull()
            is GroupTree.LayerNode -> node.layer as? SpriteLayer
            else -> null
        } ?: return SuccessResponse.Error("Node does not have any Sprite Layer")

        val canonicalPartMap = prioritySpriteLayer.parts
            .associate { Pair(it.partName, it.depth) }.toMutableMap()
        val allPartNames = spriteLayers
            .flatMap { it.parts }
            .map { it.partName }
            .distinct()

        TODO("Not yet implemented")

    }

    class SuccessResponse(
        val errors: List<String>? = null,
        val warnings: List<String>? = null )
    {
        companion object {
            fun Error(error: String) = SuccessResponse(errors = SinglyList(error))
            fun Warning(warning: String) = SuccessResponse(warnings = SinglyList(warning))
        }
    }
}