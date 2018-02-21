package spirite.base.imageData.layers

import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.mediums.BuildingMediumData
import spirite.base.imageData.mediums.IMedium
import spirite.base.util.groupExtensions.SinglyList

class SimpleLayer(val medium: MediumHandle) : Layer() {
    override val width: Int get() = medium.width
    override val height: Int get() = medium.height

    override val activeData: BuildingMediumData get() = BuildingMediumData(medium)

    override fun getDrawer(building: BuildingMediumData, medium: IMedium) = medium.getImageDrawer(building)

    override val imageDependencies: List<MediumHandle> get() = SinglyList(medium)

    override fun getDrawList(): List<TransformedHandle> = SinglyList(TransformedHandle(medium))

}