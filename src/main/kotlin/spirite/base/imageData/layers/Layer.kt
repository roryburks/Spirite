package spirite.base.imageData.layers

import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.IMedium
import spirite.base.imageData.mediums.drawer.IImageDrawer

abstract class Layer {
    abstract val width: Int
    abstract val height: Int
    abstract val activeData : ArrangedMediumData
    abstract fun getDrawer(arranged: ArrangedMediumData) : IImageDrawer
    abstract val imageDependencies : List<MediumHandle>
    abstract fun getDrawList() : List<TransformedHandle>
    abstract fun dupe( mediumRepo : MMediumRepository) : Layer


    protected fun triggerChange() {

    }
}