package spirite.base.imageData.groupTree

import spirite.base.graphics.DynamicImage
import spirite.base.graphics.RawImage
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.groupTree.GroupTree.GroupNode
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.IMedium.MediumType
import spirite.base.imageData.mediums.IMedium.MediumType.DYNAMIC
import spirite.base.imageData.mediums.IMedium.MediumType.FLAT
import spirite.base.util.MUtil
import spirite.base.util.StringUtil
import spirite.hybrid.EngineLaunchpoint

class PrimaryGroupTree(
        val workspace: IImageWorkspace,
        val mediumRepo : MMediumRepository
) : GroupTree( workspace.undoEngine) {

    fun addNewSimpleLayer( contextNode: Node, name: String, type: MediumType, width: Int? = null, height: Int? = null) : LayerNode{
        val medium = when( type) {
            DYNAMIC -> DynamicMedium(DynamicImage(),workspace)
            FLAT -> FlatMedium( EngineLaunchpoint.createImage( width ?: workspace.width, height ?: workspace.height))
            else -> FlatMedium( EngineLaunchpoint.createImage( width ?: workspace.width, height ?: workspace.height))
        }

        val handle = mediumRepo.addMedium( medium)
        val layer = SimpleLayer(handle)
        val layerNode = LayerNode( null, getNonduplicateName(name), layer)

        insertLayer( contextNode, layerNode)

        return layerNode
    }

    private fun insertLayer( contextNode: Node, layerNode: LayerNode) {
        val parent = parentOfContext(contextNode)
        val before = beforeContext(contextNode)
        parent.add( layerNode, before)
    }

    private fun getNonduplicateName( name: String) = StringUtil.getNonDuplicateName(root.getAllAncestors().map { it.name }.toList(), name)

}