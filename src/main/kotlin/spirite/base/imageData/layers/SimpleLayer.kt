package spirite.base.imageData.layers

import rb.extendo.dataStructures.SinglyList
import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.IIsolator
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.mediums.ArrangedMediumData

class SimpleLayer(val medium: MediumHandle) : Layer {
    override val x: Int get() = medium.x
    override val y: Int get() = medium.y
    override val width: Int get() = medium.width
    override val height: Int get() = medium.height

    override val activeData: ArrangedMediumData get() = ArrangedMediumData(medium)

    override fun getDrawer(arranged: ArrangedMediumData) = arranged.handle.medium.getImageDrawer(arranged)

    override val imageDependencies: List<MediumHandle> get() = SinglyList(medium)

    override fun getDrawList(isolator: IIsolator?): List<TransformedHandle> = SinglyList(TransformedHandle(medium))


    override fun dupe(workspace: MImageWorkspace): Layer {
        return SimpleLayer(workspace.mediumRepository.addMedium(medium.medium.dupe()))
    }
}