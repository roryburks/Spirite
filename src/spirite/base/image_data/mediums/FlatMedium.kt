package spirite.base.image_data.mediums

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.IImage
import spirite.base.graphics.RawImage
import spirite.base.image_data.ImageWorkspace
import spirite.base.image_data.ImageWorkspace.BuildingMediumData
import spirite.base.image_data.mediums.drawer.DefaultImageDrawer
import spirite.base.image_data.mediums.drawer.IImageDrawer
import spirite.base.util.MUtil
import spirite.base.util.linear.MatTrans
import spirite.base.util.linear.MatTrans.NoninvertableException
import spirite.base.util.linear.Rect
import spirite.base.util.linear.Vec2
import spirite.base.util.linear.Vec2i
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

    override fun build(building: BuildingMediumData): ABuiltMediumData {
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

    inner class BuiltImageData(building: BuildingMediumData) : ABuiltMediumData(building.handle) {
        internal var trans: MatTrans
        internal var invTrans: MatTrans

        init {
            this.trans = building.trans
            try {
                this.invTrans = trans.createInverse()
            } catch (e: NoninvertableException) {
                this.invTrans = MatTrans()
            }

        }

        override fun getWidth(): Int {
            return handle.width
        }

        override fun getHeight(): Int {
            return handle.height
        }

        override fun draw(gc: GraphicsContext) {
            handle.drawLayer(gc, trans)
        }

        override fun drawBorder(gc: GraphicsContext) {
            if (handle == null) return

            val transform = gc.transform
            gc.preTransform(trans)

            gc.drawRect(0, 0, handle.width, handle.height)

            gc.transform = transform
        }

        override fun convert(p: Vec2): Vec2 {
            //	Some image modification methods do not use draw actions, but
            //	 rather alter the image directly.  For example a flood fill action.
            //
            return invTrans.transform(Vec2(p.x, p.y))

        }

        override fun getBounds(): Rect {
            return MUtil.circumscribeTrans(Rect(0, 0, width, height), trans)
        }

        override fun getScreenToImageTransform(): MatTrans {
            return MatTrans(invTrans)
        }

        override fun getCompositeTransform(): MatTrans {
            return MatTrans()
        }

        override fun _doOnGC(doer: ABuiltMediumData.DoerOnGC) {
            doer.Do(image.graphics)
        }

        override fun _doOnRaw(doer: ABuiltMediumData.DoerOnRaw) {
            doer.Do(image)
        }
    }

    override fun getImageDrawer(building: BuildingMediumData): IImageDrawer {
        return DefaultImageDrawer(this, building)
    }

}