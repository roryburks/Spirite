package spirite.base.imageData.groupTree

import rb.glow.img.IImage
import spirite.base.graphics.DynamicImage
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.layers.Layer
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.MediumType
import spirite.base.imageData.mediums.MediumType.*
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.util.debug.SpiriteException
import spirite.core.util.StringUtil
import spirite.sguiHybrid.Hybrid

class PrimaryGroupTree(workspace: MImageWorkspace) : MovableGroupTree( workspace) {
    enum class InsertBehavior {
        Above,
        Bellow,
        InsertTop,
        InsertBottom
    }

    override val treeDescription: String get() = "Primary Group Tree"

    fun addNewSimpleLayer(contextNode: Node?, name: String, type: MediumType, width: Int? = null, height: Int? = null, select: Boolean = true) : LayerNode {
        val medium = when( type) {
            DYNAMIC -> DynamicMedium(workspace, DynamicImage())
            FLAT -> FlatMedium( Hybrid.imageCreator.createImage( width ?: workspace.width, height ?: workspace.height), workspace.mediumRepository)
            MAGLEV -> MaglevMedium( workspace)
            else -> throw SpiriteException("Attempted to create unsupported MediumType: $type")
        }

        val handle = workspace.mediumRepository.addMedium( medium)

        return importLayer(contextNode, name, SimpleLayer(handle), select)
    }

    fun addSimpleLayerFromImage(contextNode: Node?, name: String, image: IImage, select: Boolean = true) : LayerNode {
        val med = DynamicMedium(workspace, DynamicImage(image.deepCopy()))
        val handle = workspace.mediumRepository.addMedium(med)
        return importLayer(contextNode, name, SimpleLayer(handle), select)
    }
    fun addNewSpriteLayer(contextNode: Node?, name: String, select: Boolean = true, type: MediumType = DYNAMIC) : LayerNode {
        return importLayer(contextNode, name, SpriteLayer(workspace, type), select)
    }

    fun importLayer(contextNode: Node?, name: String, layer:Layer, select: Boolean = true, behavior: InsertBehavior? = null) : LayerNode {
        val layerNode = makeLayerNode(null, getNonDuplicateName(name), layer)
        insertNode( contextNode, layerNode, behavior)

        if( select)
            selectedNode = layerNode

        return layerNode
    }


    private fun getNonDuplicateName( name: String) = StringUtil.getNonDuplicateName(root.getAllAncestors().map { it.name }.toList(), name)

}