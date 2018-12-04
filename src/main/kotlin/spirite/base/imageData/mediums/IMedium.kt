package spirite.base.imageData.mediums

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.IFlushable
import spirite.base.graphics.RawImage
import spirite.base.graphics.RenderRubric
import spirite.base.imageData.IFloatingMedium
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.imageData.mediums.IMedium.MediumType
import spirite.base.imageData.mediums.IMedium.MediumType.FLAT
import spirite.base.util.linear.ITransformF
import spirite.base.util.linear.ImmutableTransformF


/**
 * IInternalImages are a form of base data type that serves as an intermediate between
 * Layers and RawImages/CachedImages or other primary data points.
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
    fun build(arranged: ArrangedMediumData): BuiltMediumData
    fun getImageDrawer(arranged: ArrangedMediumData): IImageDrawer

    fun render( gc: GraphicsContext, render: RenderRubric? = null)

    fun dupe(): IMedium
    override fun flush()

    enum class MediumType constructor(
            // This way, these values can be used in saving and loading without failing when
            //	an Enum is removed
            val permanentCode: Int,
            // Whether or not the user can directly create them (if they'll show up on the "Create Simple Layer" screen)
            val userCreatable: Boolean = true)
    {
        FLAT(0),
        DYNAMIC(1),
        PRISMATIC(2),
        MAGLEV(3),
        DERIVED_MAGLEV(4, false);

        companion object {
            fun fromCode(code: Int): MediumType? {
                val values = MediumType.values()

                return values.indices
                        .firstOrNull { values[it].permanentCode == code }
                        ?.let { values[it] }
            }

            val creatableTypes: Array<MediumType> by lazy {
                MediumType.values().asList().filter { it.userCreatable }.toTypedArray()
            }
        }
    }
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

    override fun dupe() = this
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
}
