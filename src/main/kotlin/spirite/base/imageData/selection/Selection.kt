package spirite.base.imageData.selection

import spirite.base.graphics.GraphicsContext.Composite.DST_IN
import spirite.base.graphics.GraphicsContext.Composite.DST_OUT
import spirite.base.graphics.IImage
import spirite.base.util.Colors
import spirite.base.util.MUtil
import spirite.base.util.f
import spirite.base.util.linear.Rect
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Transform.Companion
import spirite.base.util.linear.Vec2
import spirite.base.util.round
import spirite.hybrid.ContentBoundsFinder
import spirite.hybrid.Hybrid

/**
 * Selection is essentially just an IImage and a transform.  It represents the area which is being selected and
 * not any actual data being selected.
 */
class Selection(mask: IImage, transform: Transform? = null, crop: Boolean = false){
    val mask: IImage
    val transform: Transform?
    val width get() = mask.width
    val height get() = mask.height

    init {
        if( !crop) {
            this.transform = transform
            this.mask = mask
        }
        else {
            val cropped = ContentBoundsFinder.findContentBounds(mask, 1, true)

            val maskBeingBuilt = Hybrid.imageCreator.createImage(cropped.width, cropped.height)
            maskBeingBuilt.graphics.renderImage(mask, -cropped.x, -cropped.y)

            this.mask = maskBeingBuilt
            this.transform = transform ?: Transform.TranslationMatrix(cropped.x.f, cropped.y.f)
        }
    }


    operator fun plus( other : Selection) : Selection {
        val area = transform?.let { MUtil.circumscribeTrans(Rect(width, height), it)} ?: Rect(width, height) union
            other.transform?.let { MUtil.circumscribeTrans(Rect(other.width, other.height), it)} ?: Rect(other.width, other.height)

        val image = Hybrid.imageCreator.createImage(width, height)
        val gc = image.graphics
        gc.preTranslate(-area.x.f, -area.y.f)
        gc.pushTransform()
        transform?.apply { gc.preTransform( this)}
        gc.renderImage(mask, 0, 0)
        gc.popTransform()
        other.transform?.apply{ gc.preTransform( this)}
        gc.renderImage(other.mask, 0, 0)

        return Selection(image, Transform.TranslationMatrix(area.x.f, area.y.f))
    }

    operator fun minus(other: Selection) : Selection {
        // No need to combine
        val area = Rect(width, height)

        val image = Hybrid.imageCreator.createImage(area.width, area.height)
        val gc = image.graphics
        gc.renderImage(mask, 0, 0)
        gc.composite = DST_OUT
        gc.transform = (other.transform ?: Transform.IdentityMatrix) * (transform?.invert() ?: Transform.IdentityMatrix)
        gc.renderImage(other.mask, 0, 0)

        return Selection(image, transform, true)
    }

    infix fun intersection( other: Selection) : Selection? {
        val tOtherToThis = (other.transform ?: Transform.IdentityMatrix) * (transform?.invert() ?: Transform.IdentityMatrix)
        val area = Rect(width, height) intersection MUtil.circumscribeTrans(Rect(other.width, other.height), tOtherToThis)

        if( area.isEmpty) return null

        val image = Hybrid.imageCreator.createImage(area.width, area.height)
        val gc = image.graphics
        gc.renderImage(mask, 0, 0)
        gc.composite = DST_IN
        gc.transform = tOtherToThis
        gc.renderImage(other.mask, 0, 0)

        val retTransform = Transform.TranslationMatrix(area.x.f, area.y.f) * (transform?: Transform.IdentityMatrix)
        return Selection(image, retTransform)
    }

    fun contains(x: Int, y: Int): Boolean {
        val transformed = transform?.invert()?.apply(Vec2(x.f,y.f)) ?: Vec2(x.f,y.f)
        val x = transformed.x.round
        val y = transformed.y.round

        if( x < 0 || y < 0 || x >= width || y >= width) return false

        return mask.getColor(x,y).alpha > 0f
    }
//
//    fun invert(width: Int, height: Int) : Selection {
//        val area = Rect(0,0,width, height)
//
//        val image = Hybrid.imageCreator.createImage(area.width, area.height)
//
//        val gc = image.graphics
//        gc.color = Colors.WHITE
//        gc.fillRect(0,0,width,height)
//        gc.composite = DST_OUT
//        gc.renderImage(mask, ox, oy)
//
//        return Selection(image, area.x, area.y)
//    }

    // region Lifting

    // TODO

    // endregion

    companion object {
        fun RectangleSelection( rect: Rect) : Selection {
            // Mind the 1-pixel buffer (for drawing the border)
            val img = Hybrid.imageCreator.createImage(rect.width + 2, rect.height + 2)
            val gc = img.graphics
            gc.color = Colors.WHITE
            gc.fillRect(1,1,rect.width,rect.height)

            return Selection( img, spirite.base.util.linear.Transform.TranslationMatrix(rect.x-1f, rect.y-1f))
        }
    }
}