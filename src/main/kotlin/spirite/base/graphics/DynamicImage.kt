package spirite.base.graphics

import spirite.base.graphics.GraphicsContext.Composite.SRC
import spirite.base.util.MUtil
import spirite.base.util.linear.Rect
import spirite.base.util.linear.Transform
import spirite.hybrid.ContentBoundsFinder
import spirite.hybrid.Hybrid


/**
 * DynamicImage
 * 	A wrapper for a RawImage which automatically resizes itself to
 *
 *	Although DynamicImage is essentially a medium (DynamicMedium is a thin wrapper for a DynamicImage), other Mediums and
 *  drawing sources use this dynamically-resizing behavior so it's easiest to abstract it into its own class.
 *
 * Note: There are many ways to do compositing from the composition layer to the base layer.  Can play with them later.
 */
class DynamicImage(
        raw: RawImage? = null,
        xOffset: Int = 0,
        yOffset: Int = 0)
{
    var xOffset = xOffset ; private set
    var yOffset = yOffset ; private set
    var base: RawImage? = raw ; private set

    val width get() = base?.width ?: 0
    val height get() = base?.height ?: 0


    fun drawToImage(drawer: (RawImage) -> Unit,
                    compositionWidth: Int, compositionHeight: Int,
                    tBaseToComposition: Transform = Transform.IdentityMatrix)
    {
        val checkoutContext = checkoutRaw(compositionWidth, compositionHeight, tBaseToComposition)
        drawer.invoke(checkoutContext.buffer)
        checkin(checkoutContext)
    }

    // region Checkin/Checkout
    private inner class CompositionContext(
                    val tBaseToComposition : Transform,
                    val compositionWidth : Int,
                    val compositionHeight : Int
    ){
        val buffer = Hybrid.imageCreator.createImage(compositionWidth, compositionHeight)
    }

    private fun checkoutRaw( compositionWidth: Int, compositionHeight: Int,tBaseToComposition: Transform) : CompositionContext {
        val newContext = CompositionContext(tBaseToComposition, compositionWidth, compositionHeight)

        val gc = newContext.buffer.graphics
        gc.transform(tBaseToComposition)
        val b = base
        if( b != null ) gc.renderImage(b, xOffset, yOffset)

        return newContext
    }

    private fun checkin(context: CompositionContext) {
        val w = context.compositionWidth
        val h = context.compositionHeight

        val tCompositionToBase = context.tBaseToComposition.invert()

        val drawArea = MUtil.circumscribeTrans(Rect(0,0,w,h), tCompositionToBase)
                .union(Rect(xOffset, yOffset, base?.width ?: 1, base?.height ?: 1))

        val newBaseUncropped = Hybrid.imageCreator.createImage( drawArea.width, drawArea.height)
        val gc = newBaseUncropped.graphics

        // Draw the old image
        val b = base
        if( b != null) gc.renderImage(b, xOffset - drawArea.x, yOffset - drawArea.y)

        // Clear the section of the old image that will be replaced by the new one
        gc.transform(context.tBaseToComposition)
        gc.composite = SRC
        gc.renderImage(context.buffer, 0, 0)

        val cropped = ContentBoundsFinder.findContentBounds(newBaseUncropped, 0, true)

        val newBaseCropped = if(cropped.isEmpty) null else Hybrid.imageCreator.createImage(cropped.width, cropped.height)
        newBaseCropped?.graphics?.renderImage(newBaseUncropped, -cropped.x, -cropped.y)

        xOffset = cropped.x - drawArea.x
        yOffset = cropped.y - drawArea.y

        base?.flush()
        base = newBaseCropped

        context.buffer.flush()
        newBaseUncropped.flush()
    }
    // endregion

    fun flush() {
        base?.flush()
        base = null
    }
    fun deepCopy() = DynamicImage( base?.deepCopy(), xOffset, yOffset)
}