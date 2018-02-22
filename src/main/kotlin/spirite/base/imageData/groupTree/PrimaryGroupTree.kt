package spirite.base.imageData.groupTree

import spirite.base.brains.IObservable
import spirite.base.graphics.DynamicImage
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.IMedium.MediumType
import spirite.base.imageData.mediums.IMedium.MediumType.DYNAMIC
import spirite.base.imageData.mediums.IMedium.MediumType.FLAT
import spirite.base.util.StringUtil
import spirite.debug.SpiriteException
import spirite.hybrid.EngineLaunchpoint

class PrimaryGroupTree(
        val workspace: IImageWorkspace,
        val mediumRepo : MMediumRepository
) : MovableGroupTree( workspace.undoEngine) {
    override val treeDescription: String get() = "Primary Group Tree"

    fun addNewSimpleLayer( contextNode: Node?, name: String, type: MediumType, width: Int? = null, height: Int? = null) : LayerNode{
        val medium = when( type) {
            DYNAMIC -> DynamicMedium(DynamicImage(),workspace)
            FLAT -> FlatMedium( EngineLaunchpoint.createImage( width ?: workspace.width, height ?: workspace.height))
            else -> throw SpiriteException("Attempted to create unsupported MediumType: $type")
        }

        val handle = mediumRepo.addMedium( medium)
        val layer = SimpleLayer(handle)
        val layerNode = LayerNode( null, getNonDuplicateName(name), layer)

        insertNode( contextNode, layerNode)

        return layerNode
    }


    private fun getNonDuplicateName( name: String) = StringUtil.getNonDuplicateName(root.getAllAncestors().map { it.name }.toList(), name)

}