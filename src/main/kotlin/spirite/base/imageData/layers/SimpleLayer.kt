package spirite.base.imageData.layers

import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.IMedium
import spirite.base.util.groupExtensions.SinglyList

class SimpleLayer(val medium: MediumHandle) : Layer {
    override val x: Int get() = medium.x
    override val y: Int get() = medium.y
    override val width: Int get() = medium.width
    override val height: Int get() = medium.height

    override val activeData: ArrangedMediumData get() = ArrangedMediumData(medium)

    override fun getDrawer(arranged: ArrangedMediumData) = arranged.handle.medium.getImageDrawer(arranged)

    override val imageDependencies: List<MediumHandle> get() = SinglyList(medium)

    override fun getDrawList(): List<TransformedHandle> = SinglyList(TransformedHandle(medium))


    override fun dupe(mediumRepo : MMediumRepository): Layer {
        return SimpleLayer(mediumRepo.addMedium(medium.medium.dupe()))
    }
}