package spirite.base.imageData.selection

import rb.glow.Color
import rb.glow.Colors
import rb.glow.Composite.*
import rb.glow.drawer
import rb.glow.img.IImage
import rb.glow.img.RawImage
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.d
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.round
import sgui.swing.hybrid.ContentBoundsFinder
import sgui.swing.hybrid.Hybrid
import spirite.base.util.linear.Rect
import spirite.base.util.linear.RectangleUtil
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
class Selection(mask: IImage, transform: ITransformF? = null, crop: Boolean = false){
    val mask: IImage
    val transform: ITransformF?
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
            maskBeingBuilt.graphics.renderImage(mask, -cropped.x.d, -cropped.y.d)

            this.mask = maskBeingBuilt
            this.transform = transform ?: ImmutableTransformF.Translation(cropped.x.f, cropped.y.f)
        }
    }


    operator fun plus( other : Selection) : Selection {
        val area = (transform?.let { RectangleUtil.circumscribeTrans(Rect(width, height), it)} ?: Rect(width, height)) union
                (other.transform?.let { RectangleUtil.circumscribeTrans(Rect(other.width, other.height), it)} ?: Rect(other.width, other.height))

        val image = Hybrid.imageCreator.createImage(area.width, area.height)
        val gc = image.graphics
        gc.preTranslate(-area.x.d, -area.y.d)
        gc.pushTransform()
        transform?.apply { gc.preTransform( this)}
        gc.renderImage(mask, 0.0, 0.0)
        gc.popTransform()
        other.transform?.apply{ gc.preTransform( this)}
        gc.renderImage(other.mask, 0.0, 0.0)

        return Selection(image, ImmutableTransformF.Translation(area.x.f, area.y.f))
    }

    operator fun minus(other: Selection) : Selection {
        // No need to combine
        val area = Rect(width, height)

        val image = Hybrid.imageCreator.createImage(area.width, area.height)
        val gc = image.graphics
        gc.renderImage(mask, 0.0, 0.0)
        gc.composite = DST_OUT
        gc.transform = (other.transform ?: ImmutableTransformF.Identity) * (transform?.invert() ?: ImmutableTransformF.Identity)
        gc.renderImage(other.mask, 0.0, 0.0)

        return Selection(image, transform, true)
    }

    infix fun intersection( other: Selection) : Selection? {
        val tOtherToThis = (other.transform ?: ImmutableTransformF.Identity) * (transform?.invert() ?: ImmutableTransformF.Identity)
        val area = Rect(width, height) intersection RectangleUtil.circumscribeTrans(Rect(other.width, other.height), tOtherToThis)

        if( area.isEmpty) return null

        val image = Hybrid.imageCreator.createImage(area.width, area.height)
        val gc = image.graphics
        gc.renderImage(mask, 0.0, 0.0)
        gc.composite = DST_IN
        gc.transform = tOtherToThis
        gc.renderImage(other.mask, 0.0, 0.0)

        val retTransform = ImmutableTransformF.Translation(area.x.f, area.y.f) * (transform?: ImmutableTransformF.Identity)
        return Selection(image, retTransform)
    }

    fun contains(x: Int, y: Int): Boolean {
        val transformed = transform?.invert()?.apply(Vec2f(x.f,y.f)) ?: Vec2f(x.f,y.f)
        val tx = transformed.xf.round
        val ty = transformed.yf.round

        if( tx < 0 || ty < 0 || tx >= width || ty >= width) return false

        return mask.getColor(tx,ty).alpha > 0f
    }

    fun invert(width: Int, height: Int) : Selection {
        val area = Rect(0,0,width, height)

        val image = Hybrid.imageCreator.createImage(area.width+2, area.height+2)

        val gc = image.graphics
        //gc.jcolor = Colors.WHITE
        gc.drawer.fillRect(1.0,1.0,width.d,height.d)
        gc.composite = DST_OUT
        transform?.apply { gc.transform = this }
        gc.renderImage(mask, 1.0, 1.0)

        return Selection(image, null, false)
    }


    // region Lifting
    fun lift(image: IImage, tBaseToImage: ITransformF? = null) : RawImage {
        val tSelToImage = (tBaseToImage ?: ImmutableTransformF.Identity) * (transform ?: ImmutableTransformF.Identity)
        val tImageToSel = tSelToImage.invert()
        val lifted = Hybrid.imageCreator.createImage(mask.width, mask.height)

        lifted.graphics.apply {
            renderImage(mask, 0.0, 0.0)
            transform = tImageToSel ?: ImmutableTransformF.Identity
            composite = SRC_IN
            renderImage( image, 0.0, 0.0)
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
    fun doMaskedRequiringTransform(image: RawImage, tBaseToImage: ITransformF? = null, backgroundColor: Color? = null, lambda: (RawImage, ITransformF)->Any?) : Boolean{
        val tSelToImage = (tBaseToImage ?: ImmutableTransformF.Identity) * (transform ?: ImmutableTransformF.Identity)

        val floatingArea = RectangleUtil.circumscribeTrans( Rect(mask.width, mask.height), tSelToImage) intersection Rect(image.width, image.height)
        if( floatingArea.isEmpty)
            return false

        val tImageToFloating = ImmutableTransformF.Translation(-floatingArea.x.f, -floatingArea.y.f)
        val floatingImage = Hybrid.imageCreator.createImage(floatingArea.width, floatingArea.height)
        val compositingImage = Hybrid.imageCreator.createImage(floatingArea.width, floatingArea.height)

        try {
            val tSelToFloating = tImageToFloating * tSelToImage

            // Step 1: Lift the Selection Mask out of the image (two different ways depending on if you want the out-of
            //  bounds area to have a certain jcolor, such as when filling)
            val gc = floatingImage.graphics

            if( backgroundColor != null) {
                gc.transform = ImmutableTransformF.Identity
                gc.color = backgroundColor ?: Colors.RED
                gc.drawer.fillRect(0.0, 0.0, floatingArea.width.d, floatingArea.height.d)
                gc.composite = DST_OUT
                gc.transform = tSelToFloating
                gc.renderImage(mask, 0.0, 0.0)
                gc.composite = SRC_OVER
                gc.transform = tImageToFloating
                gc.renderImage(image, 0.0, 0.0)
            }
            else {
                gc.transform = tSelToFloating
                gc.renderImage(mask,0.0,0.0)
                gc.composite = SRC_IN
                gc.transform = tImageToFloating
                gc.renderImage(image, 0.0, 0.0)
            }

            // Step 2: execute on the lifted image
            lambda(floatingImage, tImageToFloating)

            // Step 3: Lift the Selection Mask out of the drawn image (since the draw action might have gone out of the lines)
            val cgc = compositingImage.graphics
            cgc.transform = tSelToFloating
            cgc.renderImage(mask,0.0,0.0)
            cgc.composite = SRC_IN
            cgc.renderImage(floatingImage, 0.0, 0.0)

            // Step 4: Stencil the selection mask out of the image
            val igc = image.graphics
            igc.transform = tSelToImage
            igc.composite = DST_OUT
            igc.renderImage(mask, 0.0, 0.0)

            // Step 5: Fill in the empty spot with the image from step 3
            igc.transform = ImmutableTransformF.Identity
            igc.composite = SRC_OVER
            igc.renderImage(compositingImage, floatingArea.x.d, floatingArea.y.d)

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
    inline fun doMasked(image: RawImage, tBaseToImage: ITransformF? = null, backgroundColor: Color? = null, crossinline lambda: (RawImage)->Any?) : Boolean{
        return doMaskedRequiringTransform(image, tBaseToImage, backgroundColor) {raw, UNUSED -> lambda.invoke(raw)}
    }

    // endregion

    companion object {
        fun RectangleSelection( rect: Rect) : Selection {
            // Mind the 1-pixel buffer (for drawing the border)
            val img = Hybrid.imageCreator.createImage(rect.width + 2, rect.height + 2)
            val gc = img.graphics
            gc.color = Colors.WHITE
            gc.drawer.fillRect(1.0,1.0,rect.width.d,rect.height.d)

            return Selection( img, ImmutableTransformF.Translation(rect.x-1f, rect.y-1f))
        }
    }
}