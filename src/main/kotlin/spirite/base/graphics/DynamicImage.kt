package spirite.base.graphics

import spirite.base.graphics.Composite.SRC
import spirite.base.util.linear.RectangleUtil
import spirite.base.util.linear.Rect
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import spirite.hybrid.ContentBoundsFinder
import spirite.hybrid.Hybrid


/**
 * DynamicImage
 * 	A wrapper for a RawImage which automatically resizes itself to
 *
 *	Although DynamicImage is essentially a medium (DynamicMedium is a thin wrapper for a DynamicImage), other Mediums and
 *  drawing sources use this dynamically-resizing behavior so it's easiest to abstract it into its own class.
 *
 * Note: There are many ways to do compositing from the composition layers to the base layers.  Can play with them later.
 */
class DynamicImage(
        raw: RawImage? = null,
        xOffset: Int = 0,
        yOffset: Int = 0)
    :IFlushable
{
    var xOffset = xOffset ; private set
    var yOffset = yOffset ; private set
    var base: RawImage? = raw ; private set

    val width get() = base?.width ?: 0
    val height get() = base?.height ?: 0


    fun drawToImage(compositionWidth: Int,
                    compositionHeight: Int,
                    tImageToComposition: ITransformF = ImmutableTransformF.Identity,
                    drawer: (RawImage) -> Unit)
    {
        val checkoutContext = checkoutRaw(compositionWidth, compositionHeight, tImageToComposition)
        drawer.invoke(checkoutContext.buffer)
        checkin(checkoutContext)
    }

    // region Checkin/Checkout
    private inner class CompositionContext(
            val tImageToComposite: ITransformF,
            val compositionWidth : Int,
            val compositionHeight : Int)
    {
        val buffer = Hybrid.imageCreator.createImage(compositionWidth, compositionHeight)
    }

    private fun checkoutRaw(compositionWidth: Int, compositionHeight: Int, tImageToComposition: ITransformF) : CompositionContext {
        val newContext = CompositionContext(tImageToComposition, compositionWidth, compositionHeight)

        val gc = newContext.buffer.graphics
        gc.transform(tImageToComposition)
        val b = base
        if( b != null ) gc.renderImage(b, xOffset, yOffset)

        return newContext
    }

    private fun checkin(context: CompositionContext) {
        val tCompositeToImage = context.tImageToComposite.invert() ?: ImmutableTransformF.Identity


        // Step 1: Draw Composite and Base to the combining image
        val combiningBounds = RectangleUtil.circumscribeTrans(Rect(0,0, context.compositionWidth, context.compositionHeight), tCompositeToImage)
                .union( Rect(xOffset, yOffset, base?.width ?: 0, base?.height ?: 0))

        using(Hybrid.imageCreator.createImage( combiningBounds.width, combiningBounds.height)) { combiningImage ->
            val gc = combiningImage.graphics

            base?.with {
                gc.renderImage(it, xOffset - combiningBounds.x, yOffset - combiningBounds.y)
            }

            using(context.buffer) { buffer ->
                gc.transform = tCompositeToImage
                gc.preTranslate(-combiningBounds.x + 0f, -combiningBounds.y + 0f)
                gc.composite = SRC
                gc.renderImage(buffer, 0, 0)

                // Step 2: Crop the Combining Bounds
                val contentBounds = ContentBoundsFinder.findContentBounds(combiningImage, 0, true)
                val newBase = when {
                    contentBounds.isEmpty -> null
                    else -> Hybrid.imageCreator.createImage(contentBounds.width, contentBounds.height)
                }
                newBase?.graphics?.renderImage(combiningImage, -contentBounds.x, -contentBounds.y)

                xOffset = contentBounds.x + combiningBounds.x
                yOffset = contentBounds.y + combiningBounds.y

                base = newBase
            }
        }
    }
    // endregion

    override fun flush() {
        base?.flush()
        base = null
    }
    fun deepCopy() = DynamicImage( base?.deepCopy(), xOffset, yOffset)
}