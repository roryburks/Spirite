package spirite.base.image_data.mediums

import spirite.base.graphics.IImage
import spirite.base.graphics.RawImage
import spirite.base.image_data.ImageWorkspace
import spirite.base.image_data.ImageWorkspace.BuildingMediumData
import spirite.base.image_data.mediums.drawer.DefaultImageDrawer
import spirite.base.image_data.mediums.drawer.IImageDrawer
import spirite.base.util.linear.MatTrans
import spirite.hybrid.HybridUtil

/***
 * Normal Internal Image.  Has a RawImage (cached) that represents its image data
 * and that RawImage is drawn to.
 */
class FlatMedium(private val image: RawImage, protected val context: ImageWorkspace) : IMedium {

    override val dynamicX: Int = 0
    override val dynamicY: Int = 0
    override val width: Int get() = image.width
    override val height: Int get() = image.height

    override val type: IMedium.InternalImageTypes = IMedium.InternalImageTypes.NORMAL

    override fun build(building: BuildingMediumData): BuiltMediumData {
        return BuiltImageData(building)
    }

    override fun dupe(): FlatMedium {
        return FlatMedium(image.deepCopy(), context)
    }

    override fun copyForSaving(): IMedium {
        return FlatMedium(HybridUtil.copyForSaving(image), context)
    }

    internal var flushed = false
    override fun flush() {
        if (!flushed) {
            image.flush()
            flushed = true
        }
    }
    protected fun finalize() {
        flush()
    }

    override fun readOnlyAccess(): IImage {
        return image
    }

    inner class BuiltImageData(building: BuildingMediumData) : BuiltMediumData(building) {
        override val drawTrans: MatTrans get() {return invTrans}
        override val drawWidth: Int = image.width
        override val drawHeight:Int = image.height
        override val sourceTransform: MatTrans get() {return trans}
        override val sourceWidth: Int = image.width
        override val sourceHeight: Int = image.height

        override fun _doOnGC(doer: DoerOnGC) {
            doer.Do(image.graphics)
        }

        override fun _doOnRaw(doer: DoerOnRaw) {
            doer.Do(image)
        }
    }

    override fun getImageDrawer(building: BuildingMediumData): IImageDrawer {
        return DefaultImageDrawer(this, building)
    }

}