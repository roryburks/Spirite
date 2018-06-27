package spirite.base.imageData.selection

import spirite.base.graphics.GraphicsContext.Composite.*
import spirite.base.graphics.IImage
import spirite.base.graphics.RawImage
import spirite.base.util.*
import spirite.base.util.linear.Rect
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Vec2
import spirite.hybrid.ContentBoundsFinder
import spirite.hybrid.Hybrid
import kotlin.math.max

/**
 * Selection is essentially just an IImage and a transform.  It represents the area which is being selected and
 * not any actual data being selected.
 *
 * NOTE: In order for the boundary of selection to be easily drawn, Selection IImages have a one-pixel border around
 * them containing transparent pixels (unless manually constructed without enabling cropping function).
 *
 * TODO: Selections have IImages and they need to be properly flushed when no longer used, but it can be difficult
 * to determine when a Selection no longer needs to be used (as UndoableActions will often store in them).  Figure if
 * it's worth adding a more generic ImageDependency for Actions (not just Medium Dependencies) or maybe just a specific
 * one for selections.
 */
class Selection(mask: IImage, transform: Transform? = null, crop: Boolean = false){
    val mask: IImage
    val transform: Transform?
    val width get() = mask.width
    val height get() = mask.height
    val empty: Boolean

    init {
        if( !crop) {
            this.transform = transform
            this.mask = mask
            empty = false
        }
        else {
            val cropped = ContentBoundsFinder.findContentBounds(mask, 1, true)

            empty = cropped.isEmpty
            val maskBeingBuilt = Hybrid.imageCreator.createImage(max(1,cropped.width), max(1,cropped.height))
            maskBeingBuilt.graphics.renderImage(mask, -cropped.x, -cropped.y)

            this.mask = maskBeingBuilt
            this.transform = transform ?: Transform.TranslationMatrix(cropped.x.f, cropped.y.f)
        }
    }


    operator fun plus( other : Selection) : Selection {
        val area = (transform?.let { MathUtil.circumscribeTrans(Rect(width, height), it)} ?: Rect(width, height)) union
                (other.transform?.let { MathUtil.circumscribeTrans(Rect(other.width, other.height), it)} ?: Rect(other.width, other.height))

        val image = Hybrid.imageCreator.createImage(area.width, area.height)
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
        val area = Rect(width, height) intersection MathUtil.circumscribeTrans(Rect(other.width, other.height), tOtherToThis)

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
        val tx = transformed.x.round
        val ty = transformed.y.round

        if( tx < 0 || ty < 0 || tx >= width || ty >= width) return false

        return mask.getColor(tx,ty).alpha > 0f
    }

    fun invert(width: Int, height: Int) : Selection {
        val area = Rect(0,0,width, height)

        val image = Hybrid.imageCreator.createImage(area.width+2, area.height+2)

        val gc = image.graphics
        //gc.jcolor = Colors.WHITE
        gc.fillRect(1,1,width,height)
        gc.composite = DST_OUT
        transform?.apply { gc.transform = this }
        gc.renderImage(mask, 1, 1)

        return Selection(image, null, false)
    }


    // region Lifting
    fun lift( image: IImage, tBaseToImage: Transform? = null) : RawImage {
        val tSelToImage = (tBaseToImage ?: Transform.IdentityMatrix) * (transform ?: Transform.IdentityMatrix)
        val tImageToSel = tSelToImage.invert()
        val lifted = Hybrid.imageCreator.createImage(mask.width, mask.height)

        lifted.graphics.apply {
            renderImage(mask, 0, 0)
            transform = tImageToSel
            composite = SRC_IN
            renderImage( image, 0, 0)
        }

        return lifted
    }

    /**
     * @param tBaseToImage in most cases this should be a transform from Workspace Space to Image space, but in general
     *      it's a transform on top of the selection's transform that it will use to draw the selection in Image Space
     * @param backgroundColor For some reasons (specifically when doing flood fills), you might want the area that is not
     *     being selected to be a jcolor other than transparent.  Even though this region will not be drawn to the end
     *     product, the lambda still has access to it.
     *
     *     Lambda will be passed a tSelToImage, a transformation from the Selection space to the Image space.
     *
     * @return doMasked may not execute the Lambda at all (if the selection doesn't intersect the image) in which case it returns false
     */
    fun doMaskedRequiringTransform( image: RawImage, tBaseToImage: Transform? = null, backgroundColor: Color? = null, lambda: (RawImage, Transform)->Any?) : Boolean{
        val tSelToImage = (tBaseToImage ?: Transform.IdentityMatrix) * (transform ?: Transform.IdentityMatrix)

        val floatingArea = MathUtil.circumscribeTrans( Rect(mask.width, mask.height), tSelToImage) intersection Rect(image.width, image.height)
        if( floatingArea.isEmpty)
            return false

        val tImageToFloating = Transform.TranslationMatrix(-floatingArea.x.f, -floatingArea.y.f)
        val floatingImage = Hybrid.imageCreator.createImage(floatingArea.width, floatingArea.height)
        val compositingImage = Hybrid.imageCreator.createImage(floatingArea.width, floatingArea.height)

        try {
            val tSelToFloating = tImageToFloating * tSelToImage

            // Step 1: Lift the Selection Mask out of the image (two different ways depending on if you want the out-of
            //  bounds area to have a certain jcolor, such as when filling)
            val gc = floatingImage.graphics

            if( backgroundColor != null) {
                gc.transform = Transform.IdentityMatrix
                gc.color = backgroundColor ?: Colors.RED
                gc.fillRect(0, 0, floatingArea.width, floatingArea.height)
                gc.composite = DST_OUT
                gc.transform = tSelToFloating
                gc.renderImage(mask, 0, 0)
                gc.composite = SRC_OVER
                gc.transform = tImageToFloating
                gc.renderImage(image, 0, 0)
            }
            else {
                gc.transform = tSelToFloating
                gc.renderImage(mask,0,0)
                gc.composite = SRC_IN
                gc.transform = tImageToFloating
                gc.renderImage(image, 0, 0)
            }

            // Step 2: execute on the lifted image
            lambda(floatingImage, tImageToFloating)

            // Step 3: Lift the Selection Mask out of the drawn image (since the draw action might have gone out of the lines)
            val cgc = compositingImage.graphics
            cgc.transform = tSelToFloating
            cgc.renderImage(mask,0,0)
            cgc.composite = SRC_IN
            cgc.renderImage(floatingImage, 0, 0)

            // Step 4: Stencil the selection mask out of the image
            val igc = image.graphics
            igc.transform = tSelToImage
            igc.composite = DST_OUT
            igc.renderImage(mask, 0, 0)

            // Step 5: Fill in the empty spot with the image from step 3
            igc.transform = Transform.IdentityMatrix
            igc.composite = SRC_OVER
            igc.renderImage(compositingImage, floatingArea.x, floatingArea.y)

            return true
        } finally {
            floatingImage.flush()
            compositingImage.flush()
        }
    }

    /**
     * @param tBaseToImage in most cases this should be a transform from Workspace Space to Image space, but in general
     *      it's a transform on top of the selection's transform that it will use to draw the selection in Image Space
     * @param backgroundColor For some reasons (specifically when doing flood fills), you might want the area that is not
     *     being selected to be a jcolor other than transparent.  Even though this region will not be drawn to the end
     *     product, the lambda still has access to it.
     * @return doMasked may not execute the Lambda at all (if the selection doesn't intersect the image) in which case it returns false
     */
    inline fun doMasked(image: RawImage, tBaseToImage: Transform? = null, backgroundColor: Color? = null, crossinline lambda: (RawImage)->Any?) : Boolean{
        return doMaskedRequiringTransform(image, tBaseToImage, backgroundColor) {raw, UNUSED -> lambda.invoke(raw)}
    }

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