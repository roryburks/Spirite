package spirite.base.imageData.mediums

import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import spirite.base.graphics.GraphicsContext
import rb.glow.IFlushable
import rb.glow.RawImage
import spirite.base.graphics.RenderRubric
import spirite.base.imageData.IFloatingMedium
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.imageData.mediums.MediumType.FLAT
import rb.glow.color.Colors
import rb.glow.color.SColor


/**
 * IInternalImages are a form of base data type that serves as an intermediate between
 * Layers and RawImages/CachedImages or other primary data drawPoints.
 *
 * For now all IInternalImages have the same basic functionality of wrapping one or
 * more RawImage and piping them up to the image-modifying functions based on structure,
 * but in the future it's possible that IInternalImages might be made of completely
 * different kind of things such as Vector-based image data, though this would require
 * substantial modifications to the GraphicsContext Wrapper (or perhaps another such
 * object for wrapping various drawing tool functionality).
 *
 * @author Rory Burks
 */
interface IMedium : IFlushable {
    val x: Int
    val y: Int
    val width: Int
    val height: Int
    val type: MediumType

    fun getColor(x: Int, y: Int) : SColor
    fun build(arranged: ArrangedMediumData): BuiltMediumData
    fun getImageDrawer(arranged: ArrangedMediumData): IImageDrawer

    fun render( gc: GraphicsContext, render: RenderRubric? = null)

    fun dupe(workspace: MImageWorkspace): IMedium
    override fun flush()

}

abstract class IComplexMedium : IMedium {
    override fun render( gc: GraphicsContext, render: RenderRubric?) {
        drawBehindComposite(gc,render)
        drawOverComposite(gc, render)
    }

    abstract fun drawBehindComposite(gc: GraphicsContext, render: RenderRubric? = null)
    abstract fun drawOverComposite(gc: GraphicsContext, render: RenderRubric? = null)
}

object NilMedium : IMedium {
    override val x: Int get() = 0
    override val y: Int get() = 0
    override val width: Int get() = 1
    override val height: Int get() = 1
    override val type: MediumType get() = FLAT

    override fun build(arranged: ArrangedMediumData) = NilBuiltMedium(arranged)
    override fun getColor(x: Int, y: Int) = Colors.TRANSPARENT

    override fun dupe(workspace: MImageWorkspace) = this
    override fun flush() {}
    override fun getImageDrawer(arranged: ArrangedMediumData): IImageDrawer  = throw Exception("Tried to Get Drawer for NilMedium")
    override fun render( gc: GraphicsContext, render: RenderRubric?) {}

    class NilBuiltMedium(arranged: ArrangedMediumData) : BuiltMediumData(arranged, NilMMediumRepo) {
        override val tWorkspaceToComposite: ITransformF get() = ImmutableTransformF.Identity
        override val tMediumToComposite: ITransformF get() = ImmutableTransformF.Identity
        override val width: Int get() = 1
        override val height: Int get() = 1
        override fun _drawOnComposite(doer: (GraphicsContext) -> Unit) {}
        override fun _rawAccessComposite(doer: (RawImage) -> Unit) {}
    }
}

object NilMMediumRepo : MMediumRepository {
    override fun getData(i: Int): IMedium? = null
    override val dataList: List<Int> get() = listOf()
    override fun <T> floatData(i: Int, condenser: (IMedium) -> T): IFloatingMedium<T>? = null
    override fun addMedium(medium: IMedium) = throw NotImplementedError()
    override fun replaceMediumDirect(handle: MediumHandle, newMedium: IMedium)= throw NotImplementedError()
    override fun clearUnusedCache(externalDataUsed: Set<MediumHandle>) = throw NotImplementedError()
    override fun changeMedium(i: Int, runner: (IMedium) -> Unit) {}
    override fun importMap(map: Map<Int, IMedium>): Map<Int, Int> = throw NotImplementedError()
    override fun getUnused(externalDataUsed: Set<MediumHandle>) = throw NotImplementedError()
}
