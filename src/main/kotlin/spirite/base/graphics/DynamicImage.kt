package spirite.base.graphics

import rb.glow.Composite.SRC
import rb.glow.IFlushable
import rb.glow.img.RawImage
import rb.glow.using
import rb.glow.with
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.mathUtil.d
import rbJvm.glow.util.ContentBoundsFinder
import spirite.sguiHybrid.Hybrid
import spirite.base.util.linear.Rect
import spirite.base.util.linear.RectangleUtil


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
    : IFlushable
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
        if( b != null ) gc.renderImage(b, xOffset.d, yOffset.d)

        return newContext
    }

    private fun checkin(context: CompositionContext) {
        val tCompositeToImage = context.tImageToComposite.invert() ?: ImmutableTransformF.Identity


        // Step 1: Draw Composite and Base to the combining image
        val combiningBounds = RectangleUtil.circumscribeTrans(Rect(0,0, context.compositionWidth, context.compositionHeight), tCompositeToImage)
                .union( Rect(xOffset, yOffset, base?.width ?: 0, base?.height ?: 0))

        using(Hybrid.imageCreator.createImage(combiningBounds.width, combiningBounds.height)) { combiningImage ->
            val gc = combiningImage.graphics

            base?.with {
                gc.renderImage(it, xOffset - combiningBounds.x.d, yOffset - combiningBounds.y.d)
            }

            using(context.buffer) { buffer ->
                gc.transform =tCompositeToImage
                gc.preTranslate(-combiningBounds.x.d, -combiningBounds.y.d)
                gc.composite = SRC
                gc.renderImage(buffer, 0.0, 0.0)

                // Step 2: Crop the Combining Bounds
                val contentBounds = ContentBoundsFinder.findContentBounds(combiningImage, 0, true)
                val newBase = when {
                    contentBounds.w <= 0 || contentBounds.h <= 0 -> null
                    else -> Hybrid.imageCreator.createImage(contentBounds.wi, contentBounds.hi)
                }
                //println("${contentBounds.width} , ${contentBounds.height}")
                newBase?.graphics?.renderImage(combiningImage, -contentBounds.x1, -contentBounds.y1)

                xOffset = contentBounds.x1i + combiningBounds.x
                yOffset = contentBounds.y1i + combiningBounds.y

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