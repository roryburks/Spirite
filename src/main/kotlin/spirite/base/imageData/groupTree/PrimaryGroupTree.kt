package spirite.base.imageData.groupTree

import spirite.base.graphics.DynamicImage
import spirite.base.graphics.IImage
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.layers.Layer
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.IMedium.MediumType
import spirite.base.imageData.mediums.IMedium.MediumType.DYNAMIC
import spirite.base.imageData.mediums.IMedium.MediumType.FLAT
import spirite.base.util.StringUtil
import spirite.base.util.debug.SpiriteException
import spirite.hybrid.Hybrid

class PrimaryGroupTree(
        val workspace: IImageWorkspace,
        val mediumRepo : MMediumRepository
) : MovableGroupTree( workspace.undoEngine) {
    override val treeDescription: String get() = "Primary Group Tree"

    fun addNewSimpleLayer( contextNode: Node?, name: String, type: MediumType, width: Int? = null, height: Int? = null, select: Boolean = true) : LayerNode{
        val medium = when( type) {
            DYNAMIC -> DynamicMedium(workspace, DynamicImage(), mediumRepo)
            FLAT -> FlatMedium( Hybrid.imageCreator.createImage( width ?: workspace.width, height ?: workspace.height), mediumRepo)
            else -> throw SpiriteException("Attempted to create unsupported MediumType: $type")
        }

        val handle = mediumRepo.addMedium( medium)

        return importLayer(contextNode, name, SimpleLayer(handle), select)
    }

    fun addSimpleLayerFromImage( contextNode: Node?, name: String, image: IImage, select: Boolean = true) : LayerNode {
        val med = FlatMedium(image.deepCopy(), mediumRepo)
        val handle = mediumRepo.addMedium(med)
        return importLayer(contextNode, name, SimpleLayer(handle), select)
    }
    fun addNewSpriteLayer( contextNode: Node?, name: String, select: Boolean = true) : LayerNode {
        return importLayer(contextNode, name, SpriteLayer(workspace, mediumRepo), select)
    }

    fun importLayer( contextNode: Node?, name: String, layer:Layer, select: Boolean = true) : LayerNode {
        val layerNode = LayerNode(null, getNonDuplicateName(name), layer)
        insertNode( contextNode, layerNode)

        if( select)
            selectedNode = layerNode

        return layerNode
    }

    fun duplicateNode( node: Node)  {
        workspace.undoEngine.doAsAggregateAction("Duplicate Node"){
            when( node) {
                is LayerNode ->
                    LayerNode(null, getNonDuplicateName(node.name), node.layer.dupe(mediumRepo))
                            .apply { insertNode(node, this) }
                is GroupNode -> {
                    data class NodeContext( val toDupe: Node, val parentInDuper: GroupNode)
                    val dupeQueue = mutableListOf<NodeContext>()
                    val dupeRoot = this.addGroupNode(node.parent, getNonDuplicateName(node.name))

                    node.children.forEach { dupeQueue.add(NodeContext(it, dupeRoot))}

                    while (dupeQueue.any()) {
                        val next = dupeQueue.removeAt(0)

                        val dupe : Node? = when (next.toDupe) {
                            is GroupNode -> addGroupNode(next.parentInDuper, getNonDuplicateName(next.toDupe.name))
                                    .apply { next.toDupe.children.forEach { dupeQueue.add( NodeContext( it, this )) } }
                            is LayerNode -> LayerNode( null, getNonDuplicateName(next.toDupe.name), next.toDupe.layer.dupe(mediumRepo))
                                    .apply { insertNode( next.parentInDuper, this) }
                            else -> null
                        }
                    }
                }
            }
        }
    }


    private fun getNonDuplicateName( name: String) = StringUtil.getNonDuplicateName(root.getAllAncestors().map { it.name }.toList(), name)

}