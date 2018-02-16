package spirite.base.imageData.mediums

import spirite.base.graphics.*
import spirite.base.imageData.mediums.IMedium.MediumType
import spirite.base.imageData.mediums.IMedium.MediumType.FLAT
import spirite.base.imageData.mediums.drawer.IImageDrawer
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Transform.Companion


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
interface IMedium {
    val width: Int
    val height: Int
    val type: MediumType
    fun build(building: BuildingMediumData): BuiltMediumData
    fun getImageDrawer(building: BuildingMediumData): IImageDrawer

    fun draw( gc: GraphicsContext, render: RenderProperties = RenderProperties())

    fun dupe(): IMedium
    fun flush()

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
    override fun draw(gc: GraphicsContext, render: RenderProperties) {
        drawBehindStroke(gc,render)
        drawInFrontOfStroke(gc, render)
    }

    abstract fun drawBehindStroke( gc: GraphicsContext, render: RenderProperties)
    abstract fun drawInFrontOfStroke( gc: GraphicsContext, render: RenderProperties)
}

object NilMedium : IMedium {
    override val width: Int get() = 1
    override val height: Int get() = 1
    override val type: MediumType get() = FLAT

    override fun build(building: BuildingMediumData) = NilBuiltMedium(building)

    override fun dupe() = this
    override fun flush() {}
    override fun getImageDrawer(building: BuildingMediumData): IImageDrawer  = throw Exception("Tried to Get Drawer for NilMedium")
    override fun draw(gc: GraphicsContext, render: RenderProperties) {}

    class NilBuiltMedium(building: BuildingMediumData) : BuiltMediumData(building) {
        override val width: Int get() = 1
        override val height: Int get() = 1
        override val tCompositeToWorkspace: Transform get() = Companion.IdentityMatrix
        override fun _doOnGC(doer: (GraphicsContext) -> Unit) {}
        override fun _doOnRaw(doer: (RawImage, tWorkspaceToRaw: Transform) -> Unit) {}
    }
}