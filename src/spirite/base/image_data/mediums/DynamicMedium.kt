package spirite.base.image_data.mediums

import spirite.base.graphics.DynamicImage
import spirite.base.graphics.IImage
import spirite.base.graphics.RawImage
import spirite.base.image_data.ImageWorkspace
import spirite.base.image_data.ImageWorkspace.BuildingMediumData
import spirite.base.image_data.mediums.drawer.DefaultImageDrawer
import spirite.base.image_data.mediums.drawer.IImageDrawer
import spirite.base.util.linear.MatTrans
import spirite.hybrid.HybridUtil

/***
 * A Dynamic Internal Image is a kind of image that automatically resizes itself
 * to its content bounds as it is drawn on top of.  This slightly increases the
 * time it takes to commit an image change, but reduces memory overhead as well as
 * the number of pixels pushed to re-draw the Workspace
 *
 */
class DynamicMedium : IMedium {
    private val image: DynamicImage

    protected val context: ImageWorkspace
    override val width: Int get() = image.base.width
    override val height: Int get() = image.base.height
    override val dynamicX: Int get() = image.xOffset
    override val dynamicY: Int get() = image.yOffset
    override val type: IMedium.InternalImageTypes = IMedium.InternalImageTypes.DYNAMIC

    constructor(raw: RawImage, ox: Int, oy: Int, context: ImageWorkspace) {
        this.context = context
        this.image = DynamicImage(context, raw, ox, oy)
    }

    internal constructor(image: DynamicImage, context: ImageWorkspace) {
        this.context = context
        this.image = image
    }

    override fun build(building: BuildingMediumData): BuiltMediumData {
        return DynamicBuiltImageData(building)
    }

    override fun dupe(): IMedium {
        return DynamicMedium(image.deepCopy(), context)
    }

    override fun copyForSaving(): IMedium {
        return DynamicMedium(HybridUtil.copyForSaving(image.base), image.xOffset, image.yOffset, context)
    }

    private var flushed = false
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
        return image.base
    }

    override fun getImageDrawer(building: BuildingMediumData): IImageDrawer {
        return DefaultImageDrawer(this, building)
    }

    inner class DynamicBuiltImageData(building: BuildingMediumData) : BuiltMediumData(building) {
        override val drawTrans: MatTrans get() = MatTrans()
        override val drawWidth: Int get() = context.width
        override val drawHeight: Int get() = context.height
        override val sourceTransform: MatTrans get() = trans
        override val sourceWidth: Int get() = image.width
        override val sourceHeight: Int get() = image.height

        override fun _doOnGC(doer: DoerOnGC) {
            image.doOnGC( doer, trans)
        }

        override fun _doOnRaw(doer: DoerOnRaw) {
            image.doOnRaw( doer, trans)
        }
    }
}