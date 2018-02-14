package spirite.base.imageData.mediums

import spirite.base.graphics.IImage
import spirite.base.imageData.BuildingMediumData
import spirite.base.imageData.mediums.drawer.IImageDrawer


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
    val dynamicX: Int
    val dynamicY: Int
    val type: InternalImageTypes

    fun build(building: BuildingMediumData): BuiltMediumData
    fun dupe(): IMedium
    fun copyForSaving(): IMedium    // Probably not best to offload this work to individual
    // internal image types, but it's the least immediate work
    fun flush()

    fun readOnlyAccess(): IImage
    fun getImageDrawer(building: BuildingMediumData): IImageDrawer

    enum class InternalImageTypes private constructor(
            // This way, these values can be used in saving and loading without failing when
            //	an Enum is removed
            val permanentCode: Int, val userCreateable: Boolean = true
    ) {
        NORMAL(0),
        DYNAMIC(1),
        PRISMATIC(2),
        MAGLEV(3),
        DERIVED_MAGLEV(4, false);

        fun fromCode(code: Int): InternalImageTypes? {
            val values = InternalImageTypes.values()

            return values.indices
                    .firstOrNull { values[it].permanentCode == code }
                    ?.let { values[it] }
        }

        companion object {
            val creatableTypes: Array<InternalImageTypes> by lazy {
                val creatables = InternalImageTypes.values().asList()
                return@lazy creatables.filter { it.userCreateable }.toTypedArray()
            }
        }
    }

}
