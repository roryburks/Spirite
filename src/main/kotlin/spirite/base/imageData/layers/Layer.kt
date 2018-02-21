package spirite.base.imageData.layers

import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.mediums.BuildingMediumData
import spirite.base.imageData.mediums.IMedium
import spirite.base.imageData.mediums.drawer.IImageDrawer

abstract class Layer {
    abstract val width: Int
    abstract val height: Int
    abstract val activeData : BuildingMediumData
    abstract fun getDrawer(building: BuildingMediumData, medium: IMedium) : IImageDrawer
    abstract val imageDependencies : List<MediumHandle>
    abstract fun getDrawList() : List<TransformedHandle>

}