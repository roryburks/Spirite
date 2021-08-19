package spirite.base.imageData.layers.fusion

import spirite.base.graphics.drawer.IImageDrawer
import spirite.base.graphics.isolation.IIsolator
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.TransformedHandle
import spirite.base.imageData.layers.Layer
import spirite.base.imageData.mediumGroups.MediumGroup
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.IMedium

class FusionLayer : Layer{
    private val _objects = mutableListOf<FusionStructure>()

    class FusionStructure(
            val obj: MediumGroup.MediumGroupObject,
            val medium: IMedium?,
            val ox: Int,
            val oy: Int)
    {
        val x1: Int? get() = medium?.run { x + ox }
        val y1: Int? get() = medium?.run{ y + oy}
        val x2: Int? get() = medium?.run { width + x + ox }
        val y2: Int? get() = medium?.run { height + y + oy }
    }

    override val x: Int get() = _objects.mapNotNull { it.x1 }.min() ?: 0
    override val y: Int get() = _objects.mapNotNull { it.y1}.min() ?: 0
    override val width: Int get() = _objects.mapNotNull { it.x2 }.max()?.run { this - x }  ?: 0
    override val height: Int get() = _objects.mapNotNull { it.y2 }.max()?.run { this - y } ?: 0
    override val activeData: ArrangedMediumData get() = TODO("not implemented")
    override val allArrangedData: List<ArrangedMediumData> get() = TODO("not implemented")

    override fun getDrawer(arranged: ArrangedMediumData): IImageDrawer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val imageDependencies: List<MediumHandle>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun getDrawList(isolator: IIsolator?): List<TransformedHandle> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun dupe(workspace: MImageWorkspace): Layer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}