package spirite.base.brains.commands.specific.spriteLayers

import rb.extendo.dataStructures.MutableSparseArray
import rb.global.SuccessResponse
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.groupTree.GroupTree
import spirite.base.imageData.layers.sprite.SpriteLayer

object SpriteLayerNormalizer {
    /***
     * Normalizes the Sprite Layers in the group tree node so that they have a common and consistent format.
     * Behavior:
     * 1. Selects the PrioritySpriteLayer, this one will be the final say of what depth ordering to use for common parts
     * 2. Create a Canonical part map PartName->String.  Initialize it with the layer structure of the PSL
     * 3. Using a greedy algorithm, for each part name not in the Canonical part map, find its first instance in the Sprite
     *    part, finding the part prior to it (if it exists), and create a Mapping This->Previous.  If Previous is in the
     *    canonical map, add this after the previous (while incrementing ordinals as necessary).  If not, recursively act
     *    on Previous until one is found.  (note previous can be null at which point canonicalmap.first - 1 is used)
     *
     * normalizeLayersOnly : If true, will only re-map depths to be compatible.  If false, will add parts which are missing
     */
    fun normalizeSpriteLayers( node: GroupTree.Node, workspace: IImageWorkspace, normalizeLayersOnly: Boolean): SuccessResponse {
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

        val canonicalMap = getCanonicalMap(spriteLayers, prioritySpriteLayer)

        workspace.undoEngine.doAsAggregateAction("Normalize Sprite Layers in Group {${group.name}}") {
            spriteLayers.forEach { sl ->
                val remappedDepth = sl.parts.associateWith { canonicalMap[it.partName] ?: 0 }
                sl.remapDepth(remappedDepth)

                if(!normalizeLayersOnly) {
                    val existingParts = sl.parts.map { it.partName }.toSet()
                    val missingParts = canonicalMap
                        .filter { !existingParts.contains(it.key) }
                    missingParts
                        .forEach { mp -> sl.addPart( mp.key, mp.value, SpriteLayer.SpritePartAddMode.CreateIfAbsent ) }
                }
            }
        }

        return SuccessResponse()
    }

    fun getCanonicalMap(spriteLayers: List<SpriteLayer>, prioritySpriteLayer: SpriteLayer) : Map<String, Int> {
        val errorCodes = mutableListOf<Int>()
        val canonicalList = MutableSparseArray(prioritySpriteLayer.parts.map { Pair(it.depth, it.partName) })

        val partNamesToProcess = spriteLayers
            .flatMap { it.parts }
            .map { it.partName }
            .toMutableSet()
        val addedToCanon = canonicalList.values().map { it.second }.toMutableSet()
        partNamesToProcess.removeIf { addedToCanon.contains(it) }

        fun fillCanonicalMapRec(partName: String) : Int {
            if(!partNamesToProcess.remove(partName))
                return 1

            // A: figure out what part name comes before it
            val layerToMine = spriteLayers
                .firstOrNull { sl -> sl.parts.any { it.partName == partName } } ?: return 2
            val part = layerToMine.parts.firstOrNull { it.partName == partName } ?: return 3
            val partBefore = layerToMine.parts
                .filter { it.depth < part.depth }
                .maxBy { it.depth }

            // If nothing comes before it, prepend it to the front
            if( partBefore == null) {
                val firstDepth = canonicalList.values().firstOrNull()?.first ?: 1
                val depthToAddAt = firstDepth - 1
                canonicalList.set(depthToAddAt, partName)
                return 0
            }

            // Make sure the part name is already processed so that we can find it
            val ec = fillCanonicalMapRec(partBefore.partName)
            if( ec != 0) errorCodes.add(ec)
            val depthOfPartBefore = canonicalList.values()
                .firstOrNull { it.second == partBefore.partName }?.first ?: return 4

            // Move up the depth of all parts imediately after the one before
            var depthToBumpUp = depthOfPartBefore + 1
            while( canonicalList.get(depthToBumpUp) != null)
                ++depthToBumpUp
            for (index in (depthToBumpUp downTo depthOfPartBefore + 2))
                canonicalList.set(index, canonicalList.get(index-1) ?: return  5)

            canonicalList.set(depthOfPartBefore+1, partName)
            return 0
        }

        while(partNamesToProcess.any()){
            val ec = fillCanonicalMapRec(partNamesToProcess.first())
            if( ec != 0) errorCodes.add(ec)
        }

        if( errorCodes.any()){
            println("Error Codes in SpriteLayerNormalization: ${errorCodes.joinToString(", ")}" )
        }

        return canonicalList.values()
            .associate { Pair(it.second, it.first) }
    }
}